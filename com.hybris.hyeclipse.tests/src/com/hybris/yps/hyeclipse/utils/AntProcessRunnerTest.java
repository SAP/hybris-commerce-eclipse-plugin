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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class AntProcessRunnerTest {

	@Test
	public void commandsInitializeEnvironmentAndUseSameTarget() {
		final List<String> windows = ShellImporter.antCommand("cmd.exe", true);
		Assert.assertEquals("call setantenv.bat && ant updateMavenDependencies", windows.get(2));
		final List<String> unix = ShellImporter.antCommand("/bin/bash", false);
		Assert.assertEquals(". ./setantenv.sh && exec ant updateMavenDependencies", unix.get(3));
	}

	@Test(timeout = 10000)
	public void drainsBothStreamsBeforeReturning() throws Exception {
		final List<String> output = new ArrayList<>();
		final List<String> errors = new ArrayList<>();
		AntProcessRunner.execute(shell("for ((i=0; i<500; i++)); do echo out-$i; echo err-$i >&2; done"),
				new NullProgressMonitor(), output::add, errors::add);
		Assert.assertEquals(500, output.size());
		Assert.assertEquals("out-499", output.get(499));
		Assert.assertEquals(500, errors.size());
		Assert.assertEquals("err-499", errors.get(499));
		assertReadersStopped();
	}

	@Test(timeout = 10000)
	public void propagatesNonzeroExitAfterDrainingOutput() throws Exception {
		final List<String> errors = new ArrayList<>();
		try {
			AntProcessRunner.execute(shell("echo failed >&2; exit 7"), new NullProgressMonitor(),
					line -> { /* no stdout expected */ }, errors::add);
			Assert.fail("Expected failed Ant status");
		} catch (IOException e) {
			Assert.assertTrue(e.getMessage().contains("7"));
			Assert.assertEquals("failed", errors.get(0));
		}
		assertReadersStopped();
	}

	@Test(timeout = 10000)
	public void cancellationStopsShellAndChildProcess() throws Exception {
		final NullProgressMonitor monitor = new NullProgressMonitor();
		final AtomicLong childPid = new AtomicLong();
		try {
			AntProcessRunner.execute(shell("sleep 60 & echo $!; wait"), monitor, line -> {
				childPid.set(Long.parseLong(line));
				monitor.setCanceled(true);
			}, line -> { /* no stderr expected */ });
			Assert.fail("Expected cancellation");
		} catch (OperationCanceledException e) {
			Assert.assertTrue(childPid.get() > 0);
			final ProcessHandle child = ProcessHandle.of(childPid.get()).orElse(null);
			if (child != null) {
				child.onExit().get(5, TimeUnit.SECONDS);
				Assert.assertFalse(child.isAlive());
			}
		}
		assertReadersStopped();
	}

	@Test(timeout = 10000)
	public void repeatedRunsDoNotLeaveReadersAlive() throws Exception {
		for (int run = 0; run < 3; run++) {
			AntProcessRunner.execute(shell("echo done"), new NullProgressMonitor(),
					line -> { /* output not needed */ }, line -> { /* no stderr expected */ });
			assertReadersStopped();
		}
	}

	private void assertReadersStopped() {
		Assert.assertFalse(Thread.getAllStackTraces().keySet().stream()
				.anyMatch(thread -> thread.isAlive() && AntProcessRunner.OUTPUT_THREAD_NAME.equals(thread.getName())));
	}

	@Test
	public void alreadyCanceledDoesNotStartProcess() throws Exception {
		final NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		try {
			AntProcessRunner.execute(new ProcessBuilder("does-not-exist"), monitor,
					line -> { /* no output expected */ }, line -> { /* no output expected */ });
			Assert.fail("Expected cancellation before start");
		} catch (OperationCanceledException e) {
			Assert.assertTrue(monitor.isCanceled());
		}
	}

	@Test(expected = IOException.class)
	public void propagatesStartupFailure() throws Exception {
		AntProcessRunner.execute(new ProcessBuilder("does-not-exist"), new NullProgressMonitor(),
				line -> { /* no output expected */ }, line -> { /* no output expected */ });
	}

	private ProcessBuilder shell(final String command) {
		Assume.assumeTrue(new File("/bin/bash").isFile());
		return new ProcessBuilder("/bin/bash", "-c", command);
	}
}
