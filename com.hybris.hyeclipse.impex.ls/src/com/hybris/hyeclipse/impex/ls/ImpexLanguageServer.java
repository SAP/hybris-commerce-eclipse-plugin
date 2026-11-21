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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import com.hybris.hyeclipse.impex.ls.format.ImpexFormatter;
import com.hybris.hyeclipse.impex.ls.types.ImpexDocumentAnalysis;
import com.hybris.hyeclipse.impex.ls.types.ImpexLanguageFeatures;
import com.hybris.hyeclipse.impex.ls.types.ImpexLanguageFeatures.SourceTarget;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;
import com.hybris.hyeclipse.impex.ls.types.TypeSystemProvider;

/** In-memory ImpEx language services; host metadata is supplied through a non-UI adapter boundary. */
public final class ImpexLanguageServer
		implements LanguageServer, TextDocumentService, WorkspaceService, LanguageClientAware, AutoCloseable {

	private static final int MAX_PENDING_REQUESTS = 8;
	private final Map<String, DocumentSnapshot> documents = new ConcurrentHashMap<>();
	private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
	private final TypeSystemProvider types;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(MAX_PENDING_REQUESTS), runnable -> {
				final Thread thread = new Thread(runnable, "ImpEx language analysis"); //$NON-NLS-1$
				thread.setDaemon(true);
				return thread;
			});
	private volatile boolean closed;
	private LanguageClient client;
	private final Set<String> dirtyDiagnostics = new LinkedHashSet<>();
	private boolean diagnosticsRunning;
	private volatile boolean hierarchicalSymbols;
	private final ThreadPoolExecutor diagnosticExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(1), runnable -> {
				final Thread thread = new Thread(runnable, "ImpEx diagnostics"); //$NON-NLS-1$
				thread.setDaemon(true);
				return thread;
			});

	/** Creates syntax-only services for hosts without a type-system adapter. */
	public ImpexLanguageServer() {
		this(uri -> TypeCatalog.empty());
	}

	/** @param types document-scoped metadata provider, queried only on the analysis worker */
	public ImpexLanguageServer(final TypeSystemProvider types) {
		this.types = types;
	}

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
		hierarchicalSymbols = params.getCapabilities() != null && params.getCapabilities().getTextDocument() != null
				&& params.getCapabilities().getTextDocument().getDocumentSymbol() != null
				&& Boolean.TRUE.equals(params.getCapabilities().getTextDocument().getDocumentSymbol()
						.getHierarchicalDocumentSymbolSupport());
		final ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		capabilities.setPositionEncoding(PositionEncodingKind.UTF16);
		capabilities.setDocumentFormattingProvider(true);
		capabilities.setDocumentRangeFormattingProvider(true);
		capabilities.setCompletionProvider(new CompletionOptions(false,
				Arrays.asList(";", "(", "[", ",", ".", "$", "="))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		capabilities.setDefinitionProvider(true);
		capabilities.setHoverProvider(true);
		capabilities.setDocumentSymbolProvider(true);
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
	public synchronized void connect(final LanguageClient client) {
		if (!closed) {
			this.client = client;
			dirtyDiagnostics.addAll(documents.keySet());
			startDiagnostics();
		}
	}

	@Override
	public synchronized void didOpen(final DidOpenTextDocumentParams params) {
		if (!closed) {
			documents.put(params.getTextDocument().getUri(), new DocumentSnapshot(params.getTextDocument().getText(),
					params.getTextDocument().getVersion()));
			dirtyDiagnostics.add(params.getTextDocument().getUri());
			startDiagnostics();
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
				dirtyDiagnostics.remove(uri);
				publish(new PublishDiagnosticsParams(uri, new ArrayList<>()));
				return;
			}
		}
		if (!params.getContentChanges().isEmpty()) {
			documents.put(uri, new DocumentSnapshot(params.getContentChanges().getLast().getText(), version));
			dirtyDiagnostics.add(uri);
			startDiagnostics();
		}
	}

	@Override
	public synchronized void didClose(final DidCloseTextDocumentParams params) {
		final String uri = params.getTextDocument().getUri();
		documents.remove(uri);
		dirtyDiagnostics.remove(uri);
		publish(new PublishDiagnosticsParams(uri, new ArrayList<>()));
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
		return request(params.getTextDocument().getUri(), new ArrayList<>(),
				(snapshot, cancelled) -> new ImpexFormatter().format(snapshot.text, null, cancelled));
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(final DocumentRangeFormattingParams params) {
		return request(params.getTextDocument().getUri(), new ArrayList<>(),
				(snapshot, cancelled) -> new ImpexFormatter().format(snapshot.text, params.getRange(), cancelled));
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(final CompletionParams params) {
		final String uri = params.getTextDocument().getUri();
		return request(uri, completionReply(ImpexLanguageFeatures.emptyCompletion()), (snapshot, cancelled) -> {
			final TypeCatalog catalog = types.getCatalog(uri);
			final CompletionList proposals = new ImpexLanguageFeatures().complete(snapshot.text, params.getPosition(),
					catalog, cancelled);
			return completionReply(types.getCatalog(uri) == catalog ? proposals : ImpexLanguageFeatures.emptyCompletion());
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			final DefinitionParams params) {
		final String uri = params.getTextDocument().getUri();
		return request(uri, definitionReply(new ArrayList<>()), (snapshot, cancelled) -> {
			final TypeCatalog catalog = types.getCatalog(uri);
			final SourceTarget target = new ImpexLanguageFeatures().definition(snapshot.text, params.getPosition(), catalog);
			final List<Location> locations = new ArrayList<>();
			if (target != null && !cancelled.getAsBoolean() && types.getCatalog(uri) == catalog) {
				final Location location = types.resolveLocation(uri, target.getPath(), target.getLine());
				if (location != null) {
					locations.add(location);
				}
			}
			return definitionReply(locations);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(final HoverParams params) {
		final String uri = params.getTextDocument().getUri();
		return request(uri, null, (snapshot, cancelled) -> {
			final TypeCatalog catalog = types.getCatalog(uri);
			final Hover hover = new ImpexLanguageFeatures().hover(snapshot.text, params.getPosition(), catalog, cancelled);
			return types.getCatalog(uri) == catalog ? hover : null;
		});
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			final DocumentSymbolParams params) {
		return request(params.getTextDocument().getUri(), new ArrayList<>(), (snapshot, cancelled) -> {
			final List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
			for (final DocumentSymbol symbol : new ImpexDocumentAnalysis().symbols(snapshot.text, cancelled)) {
				if (hierarchicalSymbols) {
					symbols.add(Either.forRight(symbol));
				} else {
					symbols.add(Either.forLeft(new SymbolInformation(symbol.getName(), symbol.getKind(),
							new Location(params.getTextDocument().getUri(), symbol.getSelectionRange()))));
				}
			}
			return symbols;
		});
	}

	// One pump coalesces all document changes; notification bursts cannot fill the request queue.
	private void startDiagnostics() {
		if (closed || client == null || diagnosticsRunning || dirtyDiagnostics.isEmpty()) {
			return;
		}
		diagnosticsRunning = true;
		diagnosticExecutor.execute(this::drainDiagnostics);
	}

	private void drainDiagnostics() {
		try {
			while (true) {
				final String uri;
				final DocumentSnapshot snapshot;
				synchronized (this) {
					if (closed || client == null || dirtyDiagnostics.isEmpty()) {
						return;
					}
					uri = dirtyDiagnostics.iterator().next();
					dirtyDiagnostics.remove(uri);
					snapshot = documents.get(uri);
				}
				if (snapshot == null) {
					continue;
				}
				final List<Diagnostic> diagnostics = new ImpexDocumentAnalysis().diagnostics(snapshot.text,
						() -> closed || documents.get(uri) != snapshot);
				synchronized (this) {
					if (!closed && documents.get(uri) == snapshot) {
						publish(new PublishDiagnosticsParams(uri, diagnostics, snapshot.version));
					}
				}
			}
		} finally {
			synchronized (this) {
				diagnosticsRunning = false;
				startDiagnostics();
			}
		}
	}

	// All publications are serialized with document updates/close to prevent late obsolete markers.
	private void publish(final PublishDiagnosticsParams params) {
		if (!closed && client != null) {
			try {
				client.publishDiagnostics(params);
			} catch (final RuntimeException e) {
				// A disconnected transport cannot receive notifications; reconnect supplies a new client.
				client = null;
			}
		}
	}

	private Either<List<CompletionItem>, CompletionList> completionReply(final CompletionList list) {
		return Either.forRight(list);
	}

	private Either<List<? extends Location>, List<? extends LocationLink>> definitionReply(final List<Location> locations) {
		return Either.forLeft(locations);
	}

	private synchronized <T> CompletableFuture<T> request(final String uri, final T empty,
			final BiFunction<DocumentSnapshot, BooleanSupplier, T> analyze) {
		final DocumentSnapshot snapshot = documents.get(uri);
		if (closed || snapshot == null) {
			return CompletableFuture.completedFuture(empty);
		}
		final CompletableFuture<T> result = new CompletableFuture<>();
		pending.add(result);
		result.whenComplete((value, error) -> pending.remove(result));
		try {
			executor.execute(() -> {
				try {
					final T value = analyze.apply(snapshot,
							() -> result.isCancelled() || closed || documents.get(uri) != snapshot);
					synchronized (ImpexLanguageServer.this) {
						result.complete(closed || documents.get(uri) != snapshot ? empty : value);
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
		dirtyDiagnostics.clear();
		client = null;
		for (final CompletableFuture<?> request : pending) {
			request.cancel(true);
		}
		executor.shutdownNow();
		diagnosticExecutor.shutdownNow();
	}
}
