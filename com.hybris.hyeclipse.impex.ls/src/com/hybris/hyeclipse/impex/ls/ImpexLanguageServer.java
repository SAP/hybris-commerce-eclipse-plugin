/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.ls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.hybris.hyeclipse.impex.ls.format.ImpexFormatter;

/** In-memory ImpEx formatting server; never reads files or executes ImpEx code. */
public final class ImpexLanguageServer
		implements LanguageServer, TextDocumentService, WorkspaceService, AutoCloseable {

	private static final int MAX_PENDING_REQUESTS = 8;
	private final Map<String, DocumentSnapshot> documents = new ConcurrentHashMap<>();
	private final Set<CompletableFuture<List<? extends TextEdit>>> pending = ConcurrentHashMap.newKeySet();
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(MAX_PENDING_REQUESTS), runnable -> {
				final Thread thread = new Thread(runnable, "ImpEx formatting"); //$NON-NLS-1$
				thread.setDaemon(true);
				return thread;
			});
	private volatile boolean closed;

	private static final class DocumentSnapshot {
		private final String text;
		private final int version;

		private DocumentSnapshot(final String text, final int version) {
			this.text = text;
			this.version = version;
		}
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
		final ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		capabilities.setPositionEncoding(PositionEncodingKind.UTF16);
		capabilities.setDocumentFormattingProvider(true);
		capabilities.setDocumentRangeFormattingProvider(true);
		return CompletableFuture.completedFuture(new InitializeResult(capabilities));
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		close();
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void exit() {
		close();
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return this;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return this;
	}

	@Override
	public synchronized void didOpen(final DidOpenTextDocumentParams params) {
		if (!closed) {
			documents.put(params.getTextDocument().getUri(), new DocumentSnapshot(params.getTextDocument().getText(),
					params.getTextDocument().getVersion()));
		}
	}

	@Override
	public synchronized void didChange(final DidChangeTextDocumentParams params) {
		final String uri = params.getTextDocument().getUri();
		final DocumentSnapshot previous = documents.get(uri);
		final Integer version = params.getTextDocument().getVersion();
		if (closed || previous == null || version == null || version <= previous.version) {
			return;
		}
		// Full synchronization is advertised; never format stale text after an unsupported ranged change.
		for (final TextDocumentContentChangeEvent change : params.getContentChanges()) {
			if (change.getRange() != null) {
				documents.remove(uri);
				return;
			}
		}
		if (!params.getContentChanges().isEmpty()) {
			documents.put(uri, new DocumentSnapshot(params.getContentChanges().getLast().getText(), version));
		}
	}

	@Override
	public synchronized void didClose(final DidCloseTextDocumentParams params) {
		documents.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(final DidSaveTextDocumentParams params) {
		// Full document changes are authoritative; saving does not require disk access.
	}

	@Override
	public void didChangeConfiguration(final DidChangeConfigurationParams params) {
		// Alignment has a fixed, whitespace-only policy in this first formatting slice.
	}

	@Override
	public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
		// This server analyzes open document snapshots only.
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(final DocumentFormattingParams params) {
		return format(params.getTextDocument().getUri(), null);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(final DocumentRangeFormattingParams params) {
		return format(params.getTextDocument().getUri(), params.getRange());
	}

	private synchronized CompletableFuture<List<? extends TextEdit>> format(final String uri, final Range range) {
		final DocumentSnapshot snapshot = documents.get(uri);
		if (closed || snapshot == null) {
			return CompletableFuture.completedFuture(new ArrayList<>());
		}
		final CompletableFuture<List<? extends TextEdit>> result = new CompletableFuture<>();
		pending.add(result);
		result.whenComplete((value, error) -> pending.remove(result));
		try {
			executor.execute(() -> {
				try {
					final List<TextEdit> edits = new ImpexFormatter().format(snapshot.text, range,
							() -> result.isCancelled() || closed || documents.get(uri) != snapshot);
					synchronized (ImpexLanguageServer.this) {
						result.complete(closed || documents.get(uri) != snapshot ? new ArrayList<>() : edits);
					}
				} catch (final RuntimeException e) {
					result.completeExceptionally(e);
				}
			});
		} catch (final RejectedExecutionException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/** Releases document state and cancels pending analysis when the connection stops. */
	@Override
	public synchronized void close() {
		closed = true;
		documents.clear();
		for (final CompletableFuture<List<? extends TextEdit>> request : pending) {
			request.cancel(true);
		}
		executor.shutdownNow();
	}
}
