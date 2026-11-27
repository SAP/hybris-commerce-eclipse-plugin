/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.lsp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

import com.hybris.hyeclipse.impex.ls.ImpexLanguageServer;

/** Connects LSP4E to an in-process server without launching an external executable. */
public final class ImpexConnectionProvider implements StreamConnectionProvider {

	private static final int PIPE_BUFFER_SIZE = 64 * 1024;
	private PipedInputStream input;
	private PipedOutputStream output;
	private PipedInputStream serverInput;
	private PipedOutputStream serverOutput;
	private ImpexLanguageServer server;
	private ExecutorService transport;
	private Future<?> listening;

	@Override
	public synchronized void start() throws IOException {
		if (server != null) {
			return;
		}
		try {
			input = new PipedInputStream(PIPE_BUFFER_SIZE);
			serverOutput = new PipedOutputStream(input);
			serverInput = new PipedInputStream(PIPE_BUFFER_SIZE);
			output = new PipedOutputStream(serverInput);
			server = new ImpexLanguageServer(new WorkspaceTypeSystemProvider());
			transport = Executors.newFixedThreadPool(2, runnable -> {
				final Thread thread = new Thread(runnable, "ImpEx LSP transport"); //$NON-NLS-1$
				thread.setDaemon(true);
				return thread;
			});
			final Launcher<LanguageClient> launcher = new Launcher.Builder<LanguageClient>()
					.setLocalService(server).setRemoteInterface(LanguageClient.class)
					.setInput(serverInput).setOutput(serverOutput).setExecutorService(transport).create();
			server.connect(launcher.getRemoteProxy());
			listening = launcher.startListening();
		} catch (final IOException | RuntimeException e) {
			try {
				stop();
			} catch (final RuntimeException cleanup) {
				e.addSuppressed(cleanup);
			}
			throw new IOException("Could not start the ImpEx server connection", e); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized InputStream getInputStream() {
		return input;
	}

	@Override
	public synchronized OutputStream getOutputStream() {
		return output;
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}

	@Override
	public synchronized void stop() {
		if (listening != null) {
			listening.cancel(true);
		}
		IOException failure = null;
		for (final Closeable stream : new Closeable[] {output, serverOutput, input, serverInput}) {
			if (stream != null) {
				try {
					stream.close();
				} catch (final IOException e) {
					if (failure == null) {
						failure = e;
					} else {
						failure.addSuppressed(e);
					}
				}
			}
		}
		// Unblock pipe writers before taking the server lock during shutdown.
		if (server != null) {
			server.close();
		}
		if (transport != null) {
			transport.shutdownNow();
		}
		server = null;
		input = null;
		output = null;
		serverInput = null;
		serverOutput = null;
		transport = null;
		listening = null;
		if (failure != null) {
			throw new UncheckedIOException("Could not close the ImpEx server connection", failure); //$NON-NLS-1$
		}
	}

	@Override
	public boolean equals(final Object other) {
		return other != null && getClass() == other.getClass();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
