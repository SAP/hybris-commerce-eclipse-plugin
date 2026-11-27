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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.format.TextPositions;
import com.hybris.hyeclipse.impex.ls.types.ImpexLanguageFeatures;
import com.hybris.hyeclipse.impex.ls.types.ImpexLanguageFeatures.SourceTarget;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog.Attribute;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog.Type;

public class ImpexCompletionTest {

	@Test
	public void completesOperationsAndTypeNamesWithTokenReplacement() {
		Assert.assertEquals(Arrays.asList("INSERT", "INSERT_UPDATE"), labels("INS|"));
		final CompletionItem item = complete("INSERT_UPDATE Pro|duct;code").getItems().getFirst();
		Assert.assertEquals("Product", item.getLabel());
		Assert.assertEquals("demo.ProductModel", item.getDetail());
		final TextEdit edit = item.getTextEdit().getLeft();
		Assert.assertEquals(new Position(0, 14), edit.getRange().getStart());
		Assert.assertEquals(new Position(0, 21), edit.getRange().getEnd());
		Assert.assertEquals("Product", edit.getNewText());
		Assert.assertEquals(Arrays.asList("Catalog", "CatalogVersion", "Category", "Product"), labels("INSERT |"));
	}

	@Test
	public void completesAttributesIncludingInheritedQualifiers() {
		Assert.assertEquals(Arrays.asList("name"), labels("INSERT Product;na|me[lang=en]"));
		Assert.assertEquals(Arrays.asList("uid"), labels("INSERT Product;u|"));
		Assert.assertEquals(Arrays.asList("catalogVersion"), labels("INSERT Product;CAT|"));
		final CompletionItem item = complete("INSERT Product;na|me").getItems().getFirst();
		Assert.assertEquals(new Position(0, 15), item.getTextEdit().getLeft().getRange().getStart());
		Assert.assertEquals(new Position(0, 19), item.getTextEdit().getLeft().getRange().getEnd());
	}

	@Test
	public void followsNestedReferencesCollectionsAndExplicitTypePaths() {
		Assert.assertEquals(Arrays.asList("id"), labels("INSERT Product;catalogVersion(catalog(i|d),version)"));
		Assert.assertEquals(Arrays.asList("version"), labels("INSERT Product;catalogVersion(catalog(id),v|ersion)"));
		Assert.assertEquals(Arrays.asList("code"), labels("INSERT Product;supercategories(c|ode)"));
		Assert.assertEquals(Arrays.asList("version"), labels("INSERT Product;catalogVersion(CatalogVersion.v|ersion)"));
		Assert.assertEquals(Arrays.asList("id"), labels("INSERT Product;catalogVersion(catalog.i|d,version)"));
	}

	@Test
	public void modifiersStayInHeaderOrColumnContext() {
		Assert.assertEquals(Arrays.asList("parallel"), labels("INSERT Product[pa|"));
		Assert.assertEquals(Arrays.asList("path-delimiter"), labels("INSERT Product;code[pa|"));
		Assert.assertEquals(Arrays.asList("disable.interceptor.beans", "disable.interceptor.types"),
				labels("INSERT Product[disable.interce|"));
		Assert.assertEquals(Arrays.asList("unique"), labels("INSERT Unknown;code[un|"));
		Assert.assertEquals(Arrays.asList("unique"), labels("INSERT Product;code[default='a,b',un|"));
	}

	@Test
	public void completesOnlySupportedModifierValues() {
		Assert.assertEquals(Arrays.asList("true"), labels("INSERT Product;code[unique=tr|"));
		Assert.assertEquals(Arrays.asList("append", "merge", "remove"), labels("INSERT Product;groups[mode=|"));
		Assert.assertTrue(labels("INSERT Product;creationtime[dateformat=|" ).isEmpty());
		Assert.assertTrue(labels("INSERT Product;code[default='tr|" ).isEmpty());
	}

	@Test
	public void macrosAreDocumentLocalAndDefinedBeforeTheReference() {
		Assert.assertEquals(Arrays.asList("$catalog"), labels("$catalog=one\n$catalog=two\n"
				+ "INSERT Product;code\n;id;$ca|\n$country=later\n"));
		Assert.assertTrue(labels("INSERT Product;code\n;id;$ca|\n$catalog=later\n").isEmpty());
		Assert.assertTrue(labels("$catalog=$ca|").isEmpty());
		Assert.assertEquals(Arrays.asList("$catalog"), labels("$catalog=one\nINSERT Product;code[default=$ca|]"));
	}

	@Test
	public void doesNotGuessTypesInDataCommentsScriptsOrUnknownReferences() {
		for (final String source : new String[] {"INSERT Product;code\n;na|", "# $ca|", "\"# $ca|\"",
				"#% $ca|", "INSERT Unknown;na|", "INSERT Product;missing(na|)",
				"INSERT Product;@me|", "INSERT Product;&doc|", "INSERT Product;\"na|\"",
				"INSERT Product;code\\\nna|"}) {
			Assert.assertTrue(source, labels(source).isEmpty());
		}
	}

	@Test
	public void definitionsUseIndexedDeclarationLocations() {
		final SourceTarget type = definition("INSERT Pro|duct;code");
		Assert.assertEquals("/platform/ProductModel.java", type.getPath());
		Assert.assertEquals(10, type.getLine());
		final SourceTarget inherited = definition("INSERT Product;u|id");
		Assert.assertEquals("/platform/ItemModel.java", inherited.getPath());
		Assert.assertEquals(8, inherited.getLine());
		final SourceTarget nested = definition("INSERT Product;catalogVersion(catalog(i|d),version)");
		Assert.assertEquals("/platform/CatalogModel.java", nested.getPath());
		Assert.assertEquals(21, nested.getLine());
		Assert.assertNull(definition("INSERT Product;code\n;u|id"));
		Assert.assertNull(definition("INSERT Product;code[un|ique=true]"));
	}

	@Test
	public void missingIndexStillOffersOperationsModifiersAndMacros() {
		final ImpexLanguageFeatures features = new ImpexLanguageFeatures();
		Assert.assertFalse(features.complete("INS", new Position(0, 3), TypeCatalog.empty(), () -> false)
				.getItems().isEmpty());
		Assert.assertTrue(features.complete("INSERT Product;", new Position(0, 15), TypeCatalog.empty(), () -> false)
				.getItems().isEmpty());
		Assert.assertFalse(features.complete("INSERT Unknown;code[un", new Position(0, 22), TypeCatalog.empty(),
				() -> false).getItems().isEmpty());
	}

	@Test
	public void rejectsAmbiguousModelNamesRatherThanRemovingModelSuffixes() {
		final List<Type> types = new ArrayList<>(catalog().getTypes());
		types.add(new Type("OtherCategory", "other.CategoryModel", "/platform/Other.java", 1, new ArrayList<>()));
		final TypeCatalog ambiguous = new TypeCatalog(types);
		Assert.assertNull(ambiguous.referenceType("CategoryModel"));
		Assert.assertEquals("Category", ambiguous.referenceType("java.util.Collection<demo.CategoryModel>").getCode());
		Assert.assertNull(ambiguous.referenceType("java.util.Map<demo.CategoryModel, demo.ProductModel>"));
		Assert.assertNull(ambiguous.referenceType("UnknownModel"));
	}

	@Test
	public void boundsProposalsAndSupportsPrefixRefinement() {
		final List<Type> types = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			types.add(new Type(String.format("Type%03d", i), "demo.Model" + i, "/platform/Model.java", 1,
					new ArrayList<>()));
		}
		final ImpexLanguageFeatures features = new ImpexLanguageFeatures();
		final TypeCatalog catalog = new TypeCatalog(types);
		final CompletionList all = features.complete("INSERT ", new Position(0, 7), catalog, () -> false);
		Assert.assertEquals(200, all.getItems().size());
		Assert.assertTrue(all.isIncomplete());
		final CompletionList refined = features.complete("INSERT Type29", new Position(0, 13), catalog, () -> false);
		Assert.assertEquals(10, refined.getItems().size());
		Assert.assertFalse(refined.isIncomplete());
	}

	@Test
	public void handlesCancellationInvalidPositionsAndUtf16ReplacementRanges() {
		final ImpexLanguageFeatures features = new ImpexLanguageFeatures();
		Assert.assertTrue(features.complete("INSERT Product;", new Position(0, 15), catalog(), () -> true)
				.getItems().isEmpty());
		Assert.assertTrue(features.complete("INSERT Product;", new Position(5, 0), catalog(), () -> false)
				.getItems().isEmpty());
		final CompletionItem item = complete("# é😀\r\nINSERT Product;na|me").getItems().getFirst();
		Assert.assertEquals(new Position(1, 15), item.getTextEdit().getLeft().getRange().getStart());
		Assert.assertEquals(new Position(1, 19), item.getTextEdit().getLeft().getRange().getEnd());
	}

	private CompletionList complete(final String marked) {
		final int offset = marked.indexOf('|');
		final String text = marked.replace("|", "");
		return new ImpexLanguageFeatures().complete(text, new TextPositions(text).positionAt(offset), catalog(), () -> false);
	}

	private List<String> labels(final String marked) {
		return complete(marked).getItems().stream().map(CompletionItem::getLabel).collect(Collectors.toList());
	}

	private SourceTarget definition(final String marked) {
		final int offset = marked.indexOf('|');
		final String text = marked.replace("|", "");
		return new ImpexLanguageFeatures().definition(text, new TextPositions(text).positionAt(offset), catalog());
	}

	static TypeCatalog catalog() {
		return new TypeCatalog(Arrays.asList(
				new Type("Product", "demo.ProductModel", "/platform/ProductModel.java", 10, Arrays.asList(
						new Attribute("code", "java.lang.String", "/platform/ProductModel.java", 16),
						new Attribute("name", "java.lang.String", "/platform/ProductModel.java", 18),
						new Attribute("uid", "java.lang.String", "/platform/ItemModel.java", 8),
						new Attribute("catalogVersion", "demo.CatalogVersionModel", "/platform/ProductModel.java", 20),
						new Attribute("supercategories", "java.util.Collection<demo.CategoryModel>",
								"/platform/ProductModel.java", 24))),
				new Type("CatalogVersion", "demo.CatalogVersionModel", "/platform/CatalogVersionModel.java", 10,
						Arrays.asList(new Attribute("catalog", "demo.CatalogModel", "/platform/CatalogVersionModel.java", 21),
								new Attribute("version", "java.lang.String", "/platform/CatalogVersionModel.java", 23))),
				new Type("Catalog", "demo.CatalogModel", "/platform/CatalogModel.java", 10,
						Arrays.asList(new Attribute("id", "java.lang.String", "/platform/CatalogModel.java", 21))),
				new Type("Category", "demo.CategoryModel", "/platform/CategoryModel.java", 10,
						Arrays.asList(new Attribute("code", "java.lang.String", "/platform/CategoryModel.java", 21)))));
	}
}
