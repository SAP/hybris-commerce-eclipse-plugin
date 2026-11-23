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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.ImpexLanguageServer;

public class ImpexDiagnosticsServiceTest {

	private static final String URI = "file:///diagnostics.impex";
	private static final String INVALID = "INSERT T;a;b\n;id;\"unclosed";
	private static final String VALID = "INSERT T;a;b\n;id;value";

	@Test
	public void publishesQuoteErrorsAndClearsThemAfterCorrectionAndClose() throws Exception {
		final BlockingQueue<PublishDiagnosticsParams> notifications = new LinkedBlockingQueue<>();
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			server.connect(client(notifications));
			open(server, 1, INVALID);
			final PublishDiagnosticsParams error = next(notifications);
			Assert.assertEquals(Integer.valueOf(1), error.getVersion());
			Assert.assertEquals("impex.unclosedQuote", error.getDiagnostics().getFirst().getCode().getLeft());
			change(server, 2, VALID);
			final PublishDiagnosticsParams corrected = next(notifications);
			Assert.assertEquals(Integer.valueOf(2), corrected.getVersion());
			Assert.assertTrue(corrected.getDiagnostics().isEmpty());
			server.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(URI)));
			final PublishDiagnosticsParams closed = next(notifications);
			Assert.assertNull(closed.getVersion());
			Assert.assertTrue(closed.getDiagnostics().isEmpty());
		}
	}

	@Test
	public void coalescesRapidChangesAndDoesNotPublishOldMarkersAfterReopening() throws Exception {
		final BlockingQueue<PublishDiagnosticsParams> notifications = new LinkedBlockingQueue<>();
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			server.connect(client(notifications));
			open(server, 1, INVALID);
			for (int version = 2; version <= 100; version++) {
				change(server, version, version == 100 ? VALID : INVALID);
			}
			int previous = 0;
			while (previous < 100) {
				final PublishDiagnosticsParams update = next(notifications);
				Assert.assertTrue(update.getVersion() > previous);
				previous = update.getVersion();
				if (previous == 100) {
					Assert.assertTrue(update.getDiagnostics().isEmpty());
				}
			}
			server.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(URI)));
			Assert.assertNull(next(notifications).getVersion());
			open(server, 1, INVALID);
			Assert.assertEquals(Integer.valueOf(1), next(notifications).getVersion());
		}
	}

	@Test
	public void clientConnectedAfterOpenReceivesCurrentDiagnostics() throws Exception {
		final BlockingQueue<PublishDiagnosticsParams> notifications = new LinkedBlockingQueue<>();
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, INVALID);
			server.connect(client(notifications));
			Assert.assertEquals(1, next(notifications).getDiagnostics().size());
		}
	}

	@Test
	public void negotiatesFlatAndHierarchicalSymbolResponses() throws Exception {
		try (final ImpexLanguageServer flat = new ImpexLanguageServer();
				final ImpexLanguageServer hierarchical = new ImpexLanguageServer()) {
			flat.initialize(new InitializeParams()).get();
			open(flat, 1, VALID);
			Assert.assertTrue(flat.documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier(URI)))
					.get().getFirst().isLeft());
			final DocumentSymbolCapabilities symbols = new DocumentSymbolCapabilities();
			symbols.setHierarchicalDocumentSymbolSupport(true);
			final TextDocumentClientCapabilities text = new TextDocumentClientCapabilities();
			text.setDocumentSymbol(symbols);
			final ClientCapabilities capabilities = new ClientCapabilities();
			capabilities.setTextDocument(text);
			final InitializeParams params = new InitializeParams();
			params.setCapabilities(capabilities);
			hierarchical.initialize(params).get();
			open(hierarchical, 1, VALID);
			Assert.assertTrue(hierarchical.documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier(URI)))
					.get().getFirst().isRight());
		}
	}

	@Test
	public void hoverAndSymbolsWorkWithoutACommerceIndexAndDisappearOnClose() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer()) {
			open(server, 1, "$catalog=demo\nINSERT T;a;b\n;id;$catalog");
			Assert.assertTrue(server.hover(new HoverParams(new TextDocumentIdentifier(URI), new Position(2, 6)))
					.get(5, TimeUnit.SECONDS).getContents().getRight().getValue().contains("$catalog=demo"));
			Assert.assertEquals(2, server.documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier(URI)))
					.get(5, TimeUnit.SECONDS).size());
			server.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(URI)));
			Assert.assertNull(server.hover(new HoverParams(new TextDocumentIdentifier(URI), new Position(2, 6))).get());
			Assert.assertTrue(server.documentSymbol(new DocumentSymbolParams(new TextDocumentIdentifier(URI))).get().isEmpty());
		}
	}

	private LanguageClient client(final BlockingQueue<PublishDiagnosticsParams> notifications) {
		return (LanguageClient) Proxy.newProxyInstance(LanguageClient.class.getClassLoader(),
				new Class<?>[] {LanguageClient.class}, (proxy, method, args) -> {
					if ("publishDiagnostics".equals(method.getName())) {
						notifications.add((PublishDiagnosticsParams) args[0]);
					}
					return CompletableFuture.class.isAssignableFrom(method.getReturnType())
							? CompletableFuture.completedFuture(null) : null;
				});
	}

	private PublishDiagnosticsParams next(final BlockingQueue<PublishDiagnosticsParams> notifications) throws Exception {
		final PublishDiagnosticsParams notification = notifications.poll(5, TimeUnit.SECONDS);
		Assert.assertNotNull("Expected diagnostics notification", notification);
		Assert.assertEquals(URI, notification.getUri());
		return notification;
	}

	private void open(final ImpexLanguageServer server, final int version, final String text) {
		server.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(URI, "impex", version, text)));
	}

	private void change(final ImpexLanguageServer server, final int version, final String text) {
		server.didChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(URI, version),
				Arrays.asList(new TextDocumentContentChangeEvent(text))));
	}
}
