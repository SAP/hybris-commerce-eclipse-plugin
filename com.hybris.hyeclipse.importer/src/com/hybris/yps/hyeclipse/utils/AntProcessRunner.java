/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import com.hybris.hyeclipse.ytypesystem.Activator;

/** Executes Ant without blocking cancellation or leaving output readers running. */
final class AntProcessRunner {

	private static final long POLL_INTERVAL_MILLIS = 100;
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;
	static final String OUTPUT_THREAD_NAME = "hyeclipse-ant-output"; //$NON-NLS-1$

	private AntProcessRunner() {
	}

	static void execute(final ProcessBuilder builder, final IProgressMonitor monitor,
			final Consumer<String> stdout, final Consumer<String> stderr) throws IOException, InterruptedException {
		checkCanceled(monitor);
		final Process process = builder.start();
		final ExecutorService readers = Executors.newFixedThreadPool(2, task -> {
			final Thread thread = new Thread(task, OUTPUT_THREAD_NAME);
			thread.setDaemon(true);
			return thread;
		});
		try {
			process.getOutputStream().close();
			final Future<?> output = readers.submit(() -> readLines(process.getInputStream(), stdout));
			final Future<?> errors = readers.submit(() -> readLines(process.getErrorStream(), stderr));
			while (!process.waitFor(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) {
				checkCanceled(monitor);
			}
			// Process exit does not mean its buffered output has been consumed yet.
			awaitReader(output, monitor);
			awaitReader(errors, monitor);
			checkCanceled(monitor);
			if (process.exitValue() != 0) {
				Activator.log("Ant execution ended with exit code: %d.\nCommand: %s\nEnvs: %s".formatted(process.exitValue(), builder.command(), builder.environment()));
				throw new IOException("Ant exited with code %s.\nReview information in eclipse log.\nValidate error message in terminal.\nValidate in Eclipse settings if your shell is the same as you use\nValidate in Eclipse settings if your Java JDK is compatible with your SAP Commerce Cloud platform\nValidate in your `platform` folder if `%s` exits with non-zero value. ".formatted(process.exitValue(), builder.command())); //$NON-NLS-1$
			}
		} finally {
			// Kill children before the shell so cancellation cannot leave Ant orphaned.
			final List<ProcessHandle> children = process.descendants().collect(Collectors.toList());
			for (final ProcessHandle child : children) {
				child.destroyForcibly();
			}
			if (process.isAlive()) {
				process.destroyForcibly();
			}
			try {
				process.getInputStream().close();
				process.getErrorStream().close();
			} finally {
				readers.shutdownNow();
				readers.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
		}
	}

	private static void readLines(final InputStream stream, final Consumer<String> consumer) {
		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				consumer.accept(line);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read Ant output", e); //$NON-NLS-1$
		}
	}

	private static void awaitReader(final Future<?> reader, final IProgressMonitor monitor)
			throws IOException, InterruptedException {
		while (true) {
			checkCanceled(monitor);
			try {
				reader.get(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
				return;
			} catch (TimeoutException e) {
				// Poll cancellation while a child process still holds the output pipe.
			} catch (ExecutionException e) {
				throw new IOException("Failed to collect Ant output", e.getCause()); //$NON-NLS-1$
			}
		}
	}

	private static void checkCanceled(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
