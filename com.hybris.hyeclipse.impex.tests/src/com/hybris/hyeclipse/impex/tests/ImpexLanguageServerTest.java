/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.tests;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.text.Document;
import org.eclipse.lsp4e.VersionedEdits;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.ImpexLanguageServer;
import com.hybris.hyeclipse.impex.lsp.ImpexConnectionProvider;

public class ImpexLanguageServerTest {

	private static final String URI = "file:///impex-test.impex";
	private static final String SOURCE = "INSERT T;a;b\n;x;value\n";

	@Test
	public void lsp4eAppliesFormattingAsOneUndoableChange() throws Exception {
		final Document document = new Document(SOURCE);
		DocumentUndoManagerRegistry.connect(document);
		final IDocumentUndoManager undo = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
		undo.connect(this);
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			final List<? extends TextEdit> edits = server.formatting(formatting()).get();
			new VersionedEdits(document.getModificationStamp(), edits, document).apply();
			final String formatted = document.get();
			Assert.assertNotEquals(SOURCE, formatted);
			Assert.assertTrue(undo.undoable());
			undo.undo();
			Assert.assertEquals(SOURCE, document.get());
			Assert.assertFalse(undo.undoable());
			Assert.assertTrue(undo.redoable());
			undo.redo();
			Assert.assertEquals(formatted, document.get());
		} finally {
			undo.disconnect(this);
			DocumentUndoManagerRegistry.disconnect(document);
		}
	}

	@Test
	public void lsp4eRejectsEditsWhenTheEditorDocumentChanged() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			final Document document = new Document(SOURCE);
			final VersionedEdits edits = new VersionedEdits(document.getModificationStamp(),
					server.formatting(formatting()).get(), document);
			document.replace(0, 0, "# subsequent edit\n");
			final String changed = document.get();
			try {
				edits.apply();
				Assert.fail("Expected stale formatting rejection");
			} catch (final ConcurrentModificationException expected) {
				Assert.assertEquals(changed, document.get());
			}
		}
	}

	@Test
	public void advertisesOnlyImplementedServices() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			final ServerCapabilities capabilities = server.initialize(new InitializeParams()).get().getCapabilities();
			Assert.assertEquals(TextDocumentSyncKind.Full, capabilities.getTextDocumentSync().getLeft());
			Assert.assertEquals("utf-16", capabilities.getPositionEncoding());
			Assert.assertTrue(capabilities.getDocumentFormattingProvider().getLeft());
			Assert.assertTrue(capabilities.getDocumentRangeFormattingProvider().getLeft());
			Assert.assertNotNull(capabilities.getCompletionProvider());
			Assert.assertTrue(capabilities.getDefinitionProvider().getLeft());
			Assert.assertTrue(capabilities.getHoverProvider().getLeft());
			Assert.assertTrue(capabilities.getDocumentSymbolProvider().getLeft());
		}
	}

	@Test
	public void formatsOpenSnapshotsAndNeverReadsUnopenedFiles() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			Assert.assertTrue(server.formatting(formatting()).get().isEmpty());
			open(server, 1, SOURCE);
			Assert.assertEquals(2, server.formatting(formatting()).get(5, TimeUnit.SECONDS).size());
			server.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(URI)));
			Assert.assertTrue(server.formatting(formatting()).get().isEmpty());
		}
	}

	@Test
	public void fullChangesReplaceSnapshotsAndOldVersionsAreIgnored() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			change(server, 3, "# replacement\n");
			change(server, 2, SOURCE);
			Assert.assertTrue(server.formatting(formatting()).get().isEmpty());
			change(server, 4, SOURCE);
			Assert.assertFalse(server.formatting(formatting()).get().isEmpty());
		}
	}

	@Test
	public void unsupportedRangedChangesCannotLeaveStaleFormattingState() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			final TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent("new");
			change.setRange(new Range(new Position(0, 0), new Position(0, 1)));
			server.didChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(URI, 2),
					Arrays.asList(change)));
			Assert.assertTrue(server.formatting(formatting()).get().isEmpty());
		}
	}

	@Test
	public void rangeRequestsRequireTheCompleteHeaderBlock() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			final DocumentRangeFormattingParams partial = new DocumentRangeFormattingParams(
					new TextDocumentIdentifier(URI), new FormattingOptions(4, true),
					new Range(new Position(1, 0), new Position(2, 0)));
			Assert.assertTrue(server.rangeFormatting(partial).get().isEmpty());
			partial.setRange(new Range(new Position(0, 0), new Position(2, 0)));
			Assert.assertEquals(2, server.rangeFormatting(partial).get().size());
		}
	}

	@Test
	public void shutdownDropsDocumentsAndDoesNotExitTheHostJvm() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, SOURCE);
			server.shutdown().get();
			server.exit();
			open(server, 2, SOURCE);
			Assert.assertTrue(server.formatting(formatting()).get().isEmpty());
		}
	}

	@Test
	public void exchangesRealJsonRpcMessagesAndCanRestartConnection() throws Exception {
		final ImpexConnectionProvider provider = new ImpexConnectionProvider();
		Assert.assertEquals(provider, new ImpexConnectionProvider());
		Assert.assertEquals(provider.hashCode(), new ImpexConnectionProvider().hashCode());
		for (int run = 0; run < 2; run++) {
			final ExecutorService executor = Executors.newFixedThreadPool(2);
			final BlockingQueue<PublishDiagnosticsParams> diagnostics = new LinkedBlockingQueue<>();
			Future<?> listening = null;
			try {
				provider.start();
				final LanguageClient client = (LanguageClient) Proxy.newProxyInstance(LanguageClient.class.getClassLoader(),
						new Class<?>[] {LanguageClient.class}, (proxy, method, args) -> {
							if ("publishDiagnostics".equals(method.getName())) {
								diagnostics.add((PublishDiagnosticsParams) args[0]);
							}
							return CompletableFuture.class.isAssignableFrom(method.getReturnType())
									? CompletableFuture.completedFuture(null) : null;
						});
				final Launcher<LanguageServer> launcher = new Launcher.Builder<LanguageServer>()
						.setLocalService(client).setRemoteInterface(LanguageServer.class)
						.setInput(provider.getInputStream()).setOutput(provider.getOutputStream())
						.setExecutorService(executor).create();
				listening = launcher.startListening();
				final LanguageServer remote = launcher.getRemoteProxy();
				Assert.assertNotNull(remote.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS));
				remote.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
						new TextDocumentItem(URI, "impex", 1, SOURCE)));
				final List<? extends TextEdit> edits = remote.getTextDocumentService().formatting(formatting())
						.get(5, TimeUnit.SECONDS);
				Assert.assertEquals(2, edits.size());
				final PublishDiagnosticsParams firstDiagnostics = diagnostics.poll(5, TimeUnit.SECONDS);
				Assert.assertNotNull(firstDiagnostics);
				Assert.assertEquals(Integer.valueOf(1), firstDiagnostics.getVersion());
				Assert.assertTrue(firstDiagnostics.getDiagnostics().isEmpty());
				Assert.assertEquals("INSERT T", remote.getTextDocumentService().documentSymbol(
						new DocumentSymbolParams(new TextDocumentIdentifier(URI))).get(5, TimeUnit.SECONDS)
						.getFirst().getLeft().getName());
				Assert.assertEquals(4, remote.getTextDocumentService().completion(new CompletionParams(
						new TextDocumentIdentifier(URI), new Position(0, 0))).get(5, TimeUnit.SECONDS)
						.getRight().getItems().size());
				Assert.assertTrue(remote.getTextDocumentService().definition(new DefinitionParams(
						new TextDocumentIdentifier(URI), new Position(0, 1))).get(5, TimeUnit.SECONDS).getLeft().isEmpty());
				remote.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
						new VersionedTextDocumentIdentifier(URI, 2), Arrays.asList(new TextDocumentContentChangeEvent(
								"$catalog=demo\nINSERT T;a;b\n;id;$catalog\n"))));
				Assert.assertTrue(remote.getTextDocumentService().hover(new HoverParams(new TextDocumentIdentifier(URI),
						new Position(2, 6))).get(5, TimeUnit.SECONDS).getContents().getRight().getValue()
						.contains("$catalog=demo"));
				final PublishDiagnosticsParams updatedDiagnostics = diagnostics.poll(5, TimeUnit.SECONDS);
				Assert.assertNotNull(updatedDiagnostics);
				Assert.assertEquals(Integer.valueOf(2), updatedDiagnostics.getVersion());
				remote.shutdown().get(5, TimeUnit.SECONDS);
				remote.exit();
			} finally {
				if (listening != null) {
					listening.cancel(true);
				}
				provider.stop();
				executor.shutdownNow();
				Assert.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
			Assert.assertNull(provider.getInputStream());
			Assert.assertNull(provider.getOutputStream());
			provider.stop();
		}
	}

	private void open(final ImpexLanguageServer server, final int version, final String text) {
		server.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(URI, "impex", version, text)));
	}

	private void change(final ImpexLanguageServer server, final int version, final String text) {
		server.didChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(URI, version),
				Arrays.asList(new TextDocumentContentChangeEvent(text))));
	}

	private DocumentFormattingParams formatting() {
		return new DocumentFormattingParams(new TextDocumentIdentifier(URI), new FormattingOptions(4, true));
	}
}
