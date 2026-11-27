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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.ImpexLanguageServer;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;
import com.hybris.hyeclipse.impex.ls.types.TypeSystemProvider;

public class ImpexTypeServiceTest {

	private static final String URI = "file:///A/sample.impex";
	private static final String OTHER_URI = "file:///B/sample.impex";
	private static final String SOURCE = "INSERT Product;";

	@Test
	public void neverSharesTypesBetweenDocumentCatalogs() throws Exception {
		final Map<String, TypeCatalog> catalogs = new HashMap<>();
		catalogs.put(URI, catalog("alpha"));
		catalogs.put(OTHER_URI, catalog("beta"));
		try (final ImpexLanguageServer server = new ImpexLanguageServer(
				uri -> catalogs.getOrDefault(uri, TypeCatalog.empty()))) {
			open(server, URI);
			open(server, OTHER_URI);
			Assert.assertEquals(Arrays.asList("alpha"), labels(server, URI));
			Assert.assertEquals(Arrays.asList("beta"), labels(server, OTHER_URI));
		}
	}

	@Test
	public void adoptsUpdatedSharedMetadataWithoutReopeningTheDocument() throws Exception {
		final AtomicReference<TypeCatalog> snapshot = new AtomicReference<>(catalog("code"));
		try (final ImpexLanguageServer server = new ImpexLanguageServer(uri -> snapshot.get())) {
			open(server, URI);
			Assert.assertEquals(Arrays.asList("code"), labels(server, URI));
			snapshot.set(catalog("updated"));
			Assert.assertEquals(Arrays.asList("updated"), labels(server, URI));
		}
	}

	@Test
	public void discardsCompletionsIfTheIndexChangesDuringTheRequest() throws Exception {
		final AtomicInteger lookups = new AtomicInteger();
		final TypeCatalog original = catalog("original");
		final TypeCatalog updated = catalog("updated");
		try (final ImpexLanguageServer server = new ImpexLanguageServer(
				uri -> lookups.getAndIncrement() == 0 ? original : updated)) {
			open(server, URI);
			Assert.assertTrue(labels(server, URI).isEmpty());
			Assert.assertEquals(Arrays.asList("updated"), labels(server, URI));
		}
	}

	@Test
	public void discardsCompletionsIfTheDocumentChangesDuringMetadataLookup() throws Exception {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicBoolean first = new AtomicBoolean(true);
		final TypeCatalog catalog = catalog("code");
		final TypeSystemProvider provider = uri -> {
			if (first.getAndSet(false)) {
				entered.countDown();
				try {
					Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return catalog;
		};
		try (final ImpexLanguageServer server = new ImpexLanguageServer(provider)) {
			open(server, URI);
			final CompletableFuture<Either<List<CompletionItem>, CompletionList>> request = server.completion(params(URI));
			Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));
			server.didChange(new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(URI, 2),
					Arrays.asList(new TextDocumentContentChangeEvent("# changed\n"))));
			release.countDown();
			Assert.assertTrue(request.get(5, TimeUnit.SECONDS).getRight().getItems().isEmpty());
		} finally {
			release.countDown();
		}
	}

	@Test
	public void shutdownCancelsPendingCompletionRequests() throws Exception {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		try (final ImpexLanguageServer server = new ImpexLanguageServer(uri -> {
			entered.countDown();
			try {
				release.await(5, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return TypeCatalog.empty();
		})) {
			open(server, URI);
			final CompletableFuture<Either<List<CompletionItem>, CompletionList>> request = server.completion(params(URI));
			Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));
			server.close();
			Assert.assertTrue(request.isCancelled());
		} finally {
			release.countDown();
		}
	}

	private TypeCatalog catalog(final String attribute) {
		return new TypeCatalog(Arrays.asList(new TypeCatalog.Type("Product", "demo.ProductModel", "/platform/Product.java",
				1, Arrays.asList(new TypeCatalog.Attribute(attribute, "java.lang.String", "/platform/Product.java", 2)))));
	}

	private void open(final ImpexLanguageServer server, final String uri) {
		server.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(uri, "impex", 1, SOURCE)));
	}

	private CompletionParams params(final String uri) {
		return new CompletionParams(new TextDocumentIdentifier(uri), new Position(0, SOURCE.length()));
	}

	private List<String> labels(final ImpexLanguageServer server, final String uri) throws Exception {
		return server.completion(params(uri)).get(5, TimeUnit.SECONDS).getRight().getItems().stream()
				.map(CompletionItem::getLabel).collect(Collectors.toList());
	}
}
