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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;
import com.hybris.hyeclipse.commons.typesystem.YType;
import com.hybris.hyeclipse.impex.ls.ImpexLanguageServer;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;
import com.hybris.hyeclipse.impex.lsp.WorkspaceTypeSystemProvider;

public class WorkspaceTypeSystemProviderTest {

	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();
	private final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	private final List<IProject> created = new ArrayList<>();
	private final AtomicReference<TypeSystemSnapshot> snapshot = new AtomicReference<>();
	private IProject platform;
	private IFile document;
	private IFile otherDocument;
	private IFile model;
	private WorkspaceTypeSystemProvider provider;

	@Before
	public void setUp() throws Exception {
		Assume.assumeFalse(workspace.getRoot().getProject(Constants.PLATFROM).exists());
		platform = project(Constants.PLATFROM, temporary.newFolder("A", "hybris", "bin", "platform"));
		final IProject extension = project("impex-extension-" + UUID.randomUUID(),
				temporary.newFolder("A", "hybris", "bin", "custom", "store space"));
		final IProject other = project("impex-other-" + UUID.randomUUID(),
				temporary.newFolder("B", "hybris", "bin", "custom", "other"));
		document = file(extension, "sample.impex", "INSERT Product;code");
		otherDocument = file(other, "sample.impex", "INSERT Product;code");
		model = file(platform, "ProductModel.java", "\n".repeat(20));
		snapshot.set(snapshot("code"));
		provider = new WorkspaceTypeSystemProvider(workspace, snapshot::get);
	}

	@After
	public void tearDown() throws Exception {
		for (int i = created.size() - 1; i >= 0; i--) {
			created.get(i).delete(true, true, null);
		}
	}

	@Test
	public void catalogIsScopedToTheIndexedCommerceTree() {
		Assert.assertEquals("Product", provider.getCatalog(uri(document)).getTypes().iterator().next().getCode());
		Assert.assertTrue(provider.getCatalog(uri(otherDocument)).getTypes().isEmpty());
		Assert.assertTrue(provider.getCatalog("file:///unopened/sample.impex").getTypes().isEmpty());
		Assert.assertTrue(provider.getCatalog("https://example.invalid/sample.impex").getTypes().isEmpty());
		Assert.assertTrue(provider.getCatalog("not a valid URI").getTypes().isEmpty());
		Assert.assertTrue(provider.getCatalog("file:relative").getTypes().isEmpty());
		Assert.assertTrue(provider.getCatalog(null).getTypes().isEmpty());
	}

	@Test
	public void refreshesTheCatalogOnlyWhenTheSharedSnapshotChanges() {
		final TypeCatalog first = provider.getCatalog(uri(document));
		Assert.assertSame(first, provider.getCatalog(uri(document)));
		snapshot.set(snapshot("updated"));
		final TypeCatalog updated = provider.getCatalog(uri(document));
		Assert.assertNotSame(first, updated);
		Assert.assertNotNull(updated.getType("Product").getAttribute("updated"));
		Assert.assertNull(updated.getType("Product").getAttribute("code"));
		Assert.assertNotNull(first.getType("Product").getAttribute("code"));
	}

	@Test
	public void emptySnapshotsHaveStableIdentityForSyntaxOnlyAssistance() {
		final WorkspaceTypeSystemProvider empty = new WorkspaceTypeSystemProvider(workspace, TypeSystemSnapshot::empty);
		Assert.assertSame(TypeCatalog.empty(), empty.getCatalog(uri(document)));
		Assert.assertSame(TypeCatalog.empty(), empty.getCatalog(uri(document)));
	}

	@Test
	public void closedPlatformAndAmbiguousWorkspaceAliasesDoNotLeakTypes() throws Exception {
		platform.close(null);
		Assert.assertTrue(provider.getCatalog(uri(document)).getTypes().isEmpty());
		platform.open(null);
		final IFile alias = otherDocument.getProject().getFile("alias.impex");
		alias.createLink(document.getLocationURI(), IResource.NONE, null);
		Assert.assertTrue(provider.getCatalog(uri(document)).getTypes().isEmpty());
		alias.delete(true, null);
		Assert.assertFalse(provider.getCatalog(uri(document)).getTypes().isEmpty());
	}

	@Test
	public void resolvesOnlyAccessibleSourceLocationsInTheSameTree() throws Exception {
		final Location location = provider.resolveLocation(uri(document), model.getFullPath().toPortableString(), 11);
		Assert.assertEquals(uri(model), location.getUri());
		Assert.assertEquals(new Position(10, 0), location.getRange().getStart());
		Assert.assertNull(provider.resolveLocation(uri(otherDocument), model.getFullPath().toPortableString(), 11));
		Assert.assertNull(provider.resolveLocation(uri(document), otherDocument.getFullPath().toPortableString(), 11));
		Assert.assertNull(provider.resolveLocation(uri(document), "relative.java", 1));
		Assert.assertNull(provider.resolveLocation(uri(document), model.getFullPath().toPortableString(), 0));
		model.delete(true, null);
		Assert.assertNull(provider.resolveLocation(uri(document), model.getFullPath().toPortableString(), 11));
	}

	@Test
	public void serverUsesWorkspaceMetadataWithoutImportingOrExecutingCommerce() throws Exception {
		try (final ImpexLanguageServer server = new ImpexLanguageServer(provider)) {
			final String text = "INSERT Product;code";
			server.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(uri(document), "impex", 1, text)));
			Assert.assertEquals("code", server.completion(new CompletionParams(new TextDocumentIdentifier(uri(document)),
					new Position(0, 17))).get(5, TimeUnit.SECONDS).getRight().getItems().getFirst().getLabel());
			final Location target = server.definition(new DefinitionParams(new TextDocumentIdentifier(uri(document)),
					new Position(0, 16))).get(5, TimeUnit.SECONDS).getLeft().getFirst();
			Assert.assertEquals(uri(model), target.getUri());
			Assert.assertEquals(new Position(10, 0), target.getRange().getStart());
			server.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(uri(otherDocument), "impex", 1, text)));
			Assert.assertTrue(server.completion(new CompletionParams(new TextDocumentIdentifier(uri(otherDocument)),
					new Position(0, 17))).get(5, TimeUnit.SECONDS).getRight().getItems().isEmpty());
			Assert.assertTrue(server.definition(new DefinitionParams(new TextDocumentIdentifier(uri(otherDocument)),
					new Position(0, 16))).get(5, TimeUnit.SECONDS).getLeft().isEmpty());
		}
	}

	private TypeSystemSnapshot snapshot(final String attribute) {
		return new TypeSystemSnapshot(Arrays.asList(new YType("Product", "demo.ProductModel", "demo.ItemModel",
				model.getFullPath().toPortableString(), 10, Arrays.asList(new YAttributeDescriptor(attribute,
						"java.lang.String", "demo.ProductModel", model.getFullPath().toPortableString(), 11)))));
	}

	private IProject project(final String name, final File location) throws Exception {
		final IProject project = workspace.getRoot().getProject(name);
		final IProjectDescription description = workspace.newProjectDescription(name);
		description.setLocationURI(location.toURI());
		project.create(description, new NullProgressMonitor());
		created.add(project);
		project.open(null);
		return project;
	}

	private IFile file(final IProject project, final String name, final String contents) throws Exception {
		final IFile file = project.getFile(name);
		try (final ByteArrayInputStream input = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			file.create(input, false, null);
		}
		return file;
	}

	private String uri(final IFile file) {
		return file.getLocationURI().toString();
	}
}
