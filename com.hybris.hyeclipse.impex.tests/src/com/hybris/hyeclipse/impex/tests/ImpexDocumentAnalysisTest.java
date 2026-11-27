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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.format.TextPositions;
import com.hybris.hyeclipse.impex.ls.types.ImpexDocumentAnalysis;
import com.hybris.hyeclipse.impex.ls.types.ImpexLanguageFeatures;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;

public class ImpexDocumentAnalysisTest {

	@Test
	public void hoversTypesInheritedAttributesAndNestedReferences() {
		Assert.assertTrue(hover("INSERT Pro|duct;code").getContents().getRight().getValue().contains("demo.ProductModel"));
		Assert.assertTrue(hover("INSERT Product;u|id").getContents().getRight().getValue().contains("Product.uid"));
		Assert.assertTrue(hover("INSERT Product;catalogVersion(catalog(i|d),version)")
				.getContents().getRight().getValue().contains("Catalog.id"));
		Assert.assertEquals(new Position(0, 7), hover("INSERT Pro|duct;code").getRange().getStart());
		Assert.assertEquals(new Position(0, 14), hover("INSERT Pro|duct;code").getRange().getEnd());
	}

	@Test
	public void macroHoverUsesCurrentDeclarationOrLatestEarlierDefinitionAsPlainText() {
		final Hover reference = hover("$catalog=one\n$catalog=<script>two</script>\nINSERT Product;code\n;id;$cat|alog");
		Assert.assertEquals(MarkupKind.PLAINTEXT, reference.getContents().getRight().getKind());
		Assert.assertTrue(reference.getContents().getRight().getValue().contains("<script>two</script>"));
		Assert.assertFalse(reference.getContents().getRight().getValue().contains("$catalog=one"));
		Assert.assertTrue(hover("$cat|alog=one").getContents().getRight().getValue().contains("$catalog=one"));
		Assert.assertTrue(hover("$catalog=one\n$cat|alog=two").getContents().getRight().getValue().contains("$catalog=two"));
		Assert.assertNull(hover("INSERT Product;code\n;id;$cat|alog\n$catalog=later"));
		Assert.assertNull(hover("$catalog=$cat|alog"));
	}

	@Test
	public void hoverDoesNotGuessAndBoundsLongDefinitions() {
		for (final String source : new String[] {"INSERT Unk|nown;code", "INSERT Product;missing(na|me)",
				"INSERT Product;code\n;id;na|me", "# $cat|alog", "#% $cat|alog"}) {
			Assert.assertNull(source, hover(source));
		}
		final String definition = "$catalog=" + "x".repeat(5000) + "\nINSERT Product;code\n;id;$cat|alog";
		Assert.assertTrue(hover(definition).getContents().getRight().getValue().length() < 2200);
		Assert.assertTrue(hover(definition).getContents().getRight().getValue().contains("truncated"));
	}

	@Test
	public void symbolsIdentifyMacroAndHeaderDeclarationsInSourceOrder() {
		final String source = "$catalog=demo\r\nINSERT_UPDATE Product;code\r\n;id\r\n# comment\r\n"
				+ "\"$lang=en\"\r\nUPDATE Unknown;name\r\n;id;Name";
		final List<DocumentSymbol> symbols = new ImpexDocumentAnalysis().symbols(source, () -> false);
		Assert.assertEquals(Arrays.asList("$catalog", "INSERT_UPDATE Product", "$lang", "UPDATE Unknown"),
				symbols.stream().map(DocumentSymbol::getName).collect(Collectors.toList()));
		Assert.assertEquals(new Position(1, 14), symbols.get(1).getSelectionRange().getStart());
		Assert.assertEquals(new Position(1, 21), symbols.get(1).getSelectionRange().getEnd());
		Assert.assertEquals(new Position(4, 1), symbols.get(2).getSelectionRange().getStart());
		Assert.assertEquals(new Position(4, 6), symbols.get(2).getSelectionRange().getEnd());
		for (final DocumentSymbol symbol : symbols) {
			final TextPositions positions = new TextPositions(source);
			Assert.assertTrue(positions.offsetAt(symbol.getRange().getStart())
					<= positions.offsetAt(symbol.getSelectionRange().getStart()));
			Assert.assertTrue(positions.offsetAt(symbol.getSelectionRange().getEnd())
					<= positions.offsetAt(symbol.getRange().getEnd()));
		}
	}

	@Test
	public void symbolsDoNotRecognizeHeadersInsideQuotedValuesOrInferMacroScope() {
		final String source = "$a=one\n$a=two\nINSERT T;a;b\n;id;\"INSERT Other;a;b\ntext\"\nUPDATE T;a;b\n";
		Assert.assertEquals(Arrays.asList("$a", "$a", "INSERT T", "UPDATE T"),
				new ImpexDocumentAnalysis().symbols(source, () -> false).stream()
						.map(DocumentSymbol::getName).collect(Collectors.toList()));
	}

	@Test
	public void unclosedQuotesPointToTheOpeningCellNotTheEndOfFile() {
		final Diagnostic diagnostic = diagnostics("INSERT T;a;b\r\n;id;\"line1\r\nline2").getFirst();
		Assert.assertEquals("impex.unclosedQuote", diagnostic.getCode().getLeft());
		Assert.assertEquals(DiagnosticSeverity.Error, diagnostic.getSeverity());
		Assert.assertEquals(new Position(1, 4), diagnostic.getRange().getStart());
		Assert.assertEquals(new Position(1, 5), diagnostic.getRange().getEnd());
	}

	@Test
	public void textAfterQuotesPointsToTheOffendingCharacterAndDoesNotEscapeBackslashes() {
		final Diagnostic plain = diagnostics(";id;\"abc\"def").getFirst();
		Assert.assertEquals("impex.textAfterQuote", plain.getCode().getLeft());
		Assert.assertEquals(new Position(0, 9), plain.getRange().getStart());
		Assert.assertEquals(new Position(0, 10), plain.getRange().getEnd());
		Assert.assertEquals(new Position(0, 9), diagnostics(";id;\"5\\\" monitor\"").getFirst().getRange().getStart());
	}

	@Test
	public void quoteDiagnosticsUseUtf16WithoutSplittingSurrogatePairs() {
		final Diagnostic diagnostic = diagnostics("# é😀\r\n;id;\"a\"😀").getFirst();
		Assert.assertEquals(new Position(1, 7), diagnostic.getRange().getStart());
		Assert.assertEquals(new Position(1, 9), diagnostic.getRange().getEnd());
	}

	@Test
	public void validCsvAndRuntimeDependentConstructsDoNotProduceFalseErrors() {
		for (final String source : new String[] {
				";id;abc\"def\n", ";id;\"5\"\" monitor\"\n", ";id;\"multiline\nvalue\"\n",
				";id;\"value\"  ;next\n", ";id;value\\\ncontinued\n", "# a comment with a quote \"\n",
				"INSERT Unknown;a[unlessExisting=yes];missing\n;id;$unknown;<null>;<ignore>\n",
				"#% if: unknownRuntimeCondition\n;id;value\n#% endif:\n", ";id;unfinished\\"}) {
			Assert.assertTrue(source, diagnostics(source).isEmpty());
		}
	}

	@Test
	public void outputLimitsAndCancellationAreConservative() {
		final ImpexDocumentAnalysis analysis = new ImpexDocumentAnalysis();
		Assert.assertEquals(100, diagnostics(";id;\"a\"x\n".repeat(150)).size());
		Assert.assertEquals(1000, analysis.symbols("$a=x\n".repeat(1100), () -> false).size());
		Assert.assertTrue(analysis.symbols("INSERT T;a\n", () -> true).isEmpty());
		Assert.assertTrue(analysis.diagnostics(";id;\"unclosed", () -> true).isEmpty());
		Assert.assertNull(new ImpexLanguageFeatures().hover("INSERT Product;", new Position(0, 9),
				TypeCatalog.empty(), () -> true));
	}

	private Hover hover(final String marked) {
		final int offset = marked.indexOf('|');
		final String text = marked.replace("|", "");
		return new ImpexLanguageFeatures().hover(text, new TextPositions(text).positionAt(offset),
				ImpexCompletionTest.catalog(), () -> false);
	}

	private List<Diagnostic> diagnostics(final String text) {
		return new ImpexDocumentAnalysis().diagnostics(text, () -> false);
	}
}
