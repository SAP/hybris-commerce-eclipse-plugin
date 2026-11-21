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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.CsvRecord;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.Kind;
import com.hybris.hyeclipse.impex.ls.format.ImpexFormatter;
import com.hybris.hyeclipse.impex.ls.format.TextPositions;

public class ImpexFormatterTest {

	@Test
	public void alignsCompleteBlocksAndIsIdempotent() {
		final String source = "INSERT T;a;b\n;x;value\n;long;z";
		final String expected = "INSERT T; a   ; b\n        ; x   ; value\n        ; long; z";
		Assert.assertEquals(expected, format(source));
		Assert.assertEquals(expected, format(expected));
		Assert.assertTrue(new ImpexFormatter().format(expected, null, () -> false).isEmpty());
		assertSameValues(source, expected);
	}

	@Test
	public void preservesQuotesAndEmptyTrailingCells() {
		final String source = "INSERT T;a;b;c\n;id;\"  5\"\" monitor; large  \";\n;abc\"def;<ignore>;";
		final String formatted = format(source);
		Assert.assertNotEquals(source, formatted);
		Assert.assertTrue(formatted.contains("\"  5\"\" monitor; large  \""));
		assertSameValues(source, formatted);
	}

	@Test
	public void preservesScriptsCommentsAndMacroDefinitionsVerbatim() {
		final String source = "$catalog=demo\n# comment\nINSERT T;a;b\n;id;value # literal\n"
				+ "\"#% beforeEach: line.clear();\"\n\"# quoted comment\";anything\n$other=  untouched  \n;long;x\n";
		final String formatted = format(source);
		Assert.assertNotEquals(source, formatted);
		for (final String preserved : new String[] {"$catalog=demo\n", "# comment\n",
				"\"#% beforeEach: line.clear();\"\n", "\"# quoted comment\";anything\n", "$other=  untouched  \n"}) {
			Assert.assertTrue(preserved, formatted.contains(preserved));
		}
		assertSameValues(source, formatted);
	}

	@Test
	public void leavesMultilineAndBackslashContinuedBlocksUnchanged() {
		for (final String source : new String[] {
				"INSERT T;a;b\n;id;\"line1\nline2\"\n;other;value\n",
				"INSERT T;a;b\r\n;id;part1\\\r\npart2\r\n;other;value\r\n",
				"INSERT T;a;b\n;id;part1\\\nINSERT T;not;a;header\n"}) {
			Assert.assertEquals(source, format(source));
		}
	}

	@Test
	public void refusesMalformedOrAmbiguousBlocks() {
		for (final String source : new String[] {
				"INSERT T;a;b\n;id;\"abc\"def\n",
				"INSERT T;a;b\n;id;\"unclosed\n;other;x\n",
				"INSERT T;a;b\n;id;\"5\\\" monitor\"\n",
				"INSERT T;a;b\n;id\n",
				"INSERT T;a;b\n;id;value;extra\n",
				"INSERT T;a;b\nunknown line\n;id;x\n",
				"INSERT T;a;b\n;id;unfinished\\"}) {
			Assert.assertEquals(source, format(source));
		}
	}

	@Test
	public void canFormatValidBlockAfterMalformedRecord() {
		final String unsafe = "INSERT T;a;b\n;id;\"abc\"def\n";
		final String safe = "INSERT T;a;b\n;long;x\n";
		Assert.assertEquals(unsafe + format(safe), format(unsafe + safe));
	}

	@Test
	public void rangeFormattingDoesNotChangeUnselectedBlocks() {
		final String first = "INSERT T;a;b\n;x;value\n";
		final String second = "UPDATE T;a;b\n;other;value\n";
		final String source = first + second;
		final TextPositions positions = new TextPositions(source);
		final Range selection = new Range(new Position(0, 0), positions.positionAt(first.length()));
		final List<TextEdit> edits = new ImpexFormatter().format(source, selection, () -> false);
		Assert.assertEquals(format(first) + second, apply(source, edits));
		Assert.assertTrue(new ImpexFormatter().format(source,
				new Range(new Position(1, 0), new Position(2, 0)), () -> false).isEmpty());
		Assert.assertTrue(new ImpexFormatter().format(source,
				new Range(new Position(0, 0), new Position(0, 5)), () -> false).isEmpty());
	}

	@Test
	public void preservesUnicodeMixedDelimitersAndFinalNewlineState() {
		for (final String ending : new String[] {"", "\n", "\r\n"}) {
			final String source = "INSERT T;a;b\r\n;id;é😀\r;long;value" + ending;
			final String formatted = format(source);
			assertSameValues(source, formatted);
			Assert.assertEquals(source.replaceAll("[^\\r\\n]", ""), formatted.replaceAll("[^\\r\\n]", ""));
			Assert.assertEquals(source.endsWith("\n"), formatted.endsWith("\n"));
		}
	}

	@Test
	public void handlesNestedReferencesAndSpecialValuesWithoutExpansion() {
		final String source = "INSERT_UPDATE T;a[unique=true];catalogVersion(catalog(id),version);b\n"
				+ ";$code;demo:Staged;(+?) one\n;id;demo:<null>;<ignore>\n";
		assertSameValues(source, format(source));
	}

	@Test
	public void enforcesDocumentColumnAndAlignmentLimits() {
		Assert.assertEquals("x".repeat(ImpexCsvScanner.MAX_DOCUMENT_LENGTH + 1),
				format("x".repeat(ImpexCsvScanner.MAX_DOCUMENT_LENGTH + 1)));
		final String wide = "INSERT T;a;b\n;id;" + "x".repeat(257);
		Assert.assertEquals(wide, format(wide));
		final String manyColumns = "INSERT T" + ";a".repeat(ImpexCsvScanner.MAX_COLUMNS + 1);
		Assert.assertEquals(manyColumns, format(manyColumns));
	}

	@Test
	public void cancellationDiscardsAllEditsAndInvalidRangesAreSafe() {
		final String source = "INSERT T;a;b\n;x;value\n".repeat(1000);
		final AtomicInteger checks = new AtomicInteger();
		Assert.assertTrue(new ImpexFormatter().format(source, null, () -> checks.incrementAndGet() > 10).isEmpty());
		Assert.assertTrue(new ImpexFormatter().format(source,
				new Range(new Position(0, 0), new Position(5000, 0)), () -> false).isEmpty());
	}

	@Test
	public void scannerRecognizesCellQuotesAndLogicalRecordBoundaries() {
		final String source = "INSERT T;a;b\r\n;id;\"a;\"\"b\nnext\"\r\n;id;abc\"def\r\n;id;x\\\r\ny\r\n";
		final List<CsvRecord> records = new ImpexCsvScanner().scan(source);
		Assert.assertEquals(4, records.size());
		Assert.assertEquals(Kind.HEADER, records.get(0).getKind());
		Assert.assertTrue(records.get(1).isMultiline());
		Assert.assertEquals(3, records.get(1).getCells().size());
		Assert.assertFalse(records.get(2).isMultiline());
		Assert.assertTrue(records.get(2).isValid());
		Assert.assertTrue(records.get(3).isMultiline());
		int end = 0;
		for (final CsvRecord record : records) {
			Assert.assertEquals(end, record.getStart());
			end = record.getEnd();
		}
		Assert.assertEquals(source.length(), end);
	}

	@Test
	public void positionsUseUtf16AndRejectOffsetsPastLineEnds() {
		final TextPositions positions = new TextPositions("é😀B\r\nnext\rlast\n");
		Assert.assertEquals(3, positions.offsetAt(new Position(0, 3)));
		Assert.assertEquals(new Position(0, 3), positions.positionAt(3));
		Assert.assertEquals(6, positions.offsetAt(new Position(1, 0)));
		Assert.assertEquals(11, positions.offsetAt(new Position(2, 0)));
		try {
			positions.offsetAt(new Position(0, 5));
			Assert.fail("Expected invalid position");
		} catch (final IllegalArgumentException expected) {
			Assert.assertNotNull(expected.getMessage());
		}
	}

	private String format(final String source) {
		return apply(source, new ImpexFormatter().format(source, null, () -> false));
	}

	private String apply(final String source, final List<? extends TextEdit> edits) {
		final StringBuilder result = new StringBuilder(source);
		final TextPositions positions = new TextPositions(source);
		int nextStart = source.length();
		for (int i = edits.size() - 1; i >= 0; i--) {
			final TextEdit edit = edits.get(i);
			final int start = positions.offsetAt(edit.getRange().getStart());
			final int end = positions.offsetAt(edit.getRange().getEnd());
			Assert.assertTrue("Edits overlap", end <= nextStart);
			result.replace(start, end, edit.getNewText());
			nextStart = start;
		}
		return result.toString();
	}

	private void assertSameValues(final String before, final String after) {
		final List<CsvRecord> original = new ImpexCsvScanner().scan(before);
		final List<CsvRecord> formatted = new ImpexCsvScanner().scan(after);
		Assert.assertEquals(original.size(), formatted.size());
		for (int row = 0; row < original.size(); row++) {
			Assert.assertEquals(original.get(row).getKind(), formatted.get(row).getKind());
			Assert.assertEquals(original.get(row).getCells().size(), formatted.get(row).getCells().size());
			for (int col = 0; col < original.get(row).getCells().size(); col++) {
				Assert.assertEquals(ImpexCsvScanner.valueOf(original.get(row).getCells().get(col)),
						ImpexCsvScanner.valueOf(formatted.get(row).getCells().get(col)));
			}
		}
	}
}
