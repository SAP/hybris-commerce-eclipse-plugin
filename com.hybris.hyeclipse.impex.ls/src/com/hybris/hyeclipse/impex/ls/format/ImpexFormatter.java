/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.ls.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.CsvRecord;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.Kind;

/** Aligns complete, single-line header blocks using only safe CSV edge whitespace. */
public final class ImpexFormatter {

	private static final int MAX_ALIGNMENT_WIDTH = 256;

	/**
	 * Formats the document, or complete blocks inside a selection. Scripts, comments,
	 * macros, line endings, and multiline/continued blocks are never rewritten.
	 * @param source immutable document text
	 * @param selection selection, or null for the full document
	 * @param cancelled cancellation/stale-result predicate
	 * @return non-overlapping source-ordered edits; empty on unsafe input or cancellation
	 */
	public List<TextEdit> format(final String source, final Range selection, final BooleanSupplier cancelled) {
		final List<TextEdit> edits = new ArrayList<>();
		if (source.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH || cancelled.getAsBoolean()) {
			return edits;
		}
		final TextPositions positions = new TextPositions(source);
		final int from;
		final int to;
		final List<CsvRecord> records;
		try {
			from = selection == null ? 0 : positions.offsetAt(selection.getStart());
			to = selection == null ? source.length() : positions.offsetAt(selection.getEnd());
			records = new ImpexCsvScanner().scan(source, cancelled);
		} catch (final IllegalArgumentException e) {
			return edits;
		}
		long outputLength = source.length();
		for (int first = 0; first < records.size(); first++) {
			if (cancelled.getAsBoolean()) {
				return new ArrayList<>();
			}
			final CsvRecord header = records.get(first);
			if (header.getKind() != Kind.HEADER) {
				continue;
			}
			int end = first + 1;
			while (end < records.size() && records.get(end).getKind() != Kind.HEADER) {
				end++;
			}
			final List<CsvRecord> block = records.subList(first, end);
			if (header.getStart() >= from && block.getLast().getContentEnd() <= to) {
				final int[] widths = widths(block);
				if (widths != null) {
					for (final CsvRecord record : block) {
						if (cancelled.getAsBoolean()) {
							return new ArrayList<>();
						}
						if (record.getKind() != Kind.HEADER && record.getKind() != Kind.DATA) {
							continue;
						}
						final String replacement = align(record.getCells(), widths);
						final String original = source.substring(record.getStart(), record.getContentEnd());
						outputLength += replacement.length() - original.length();
						if (outputLength > ImpexCsvScanner.MAX_DOCUMENT_LENGTH) {
							return new ArrayList<>();
						}
						if (!replacement.equals(original)) {
							edits.add(new TextEdit(new Range(positions.positionAt(record.getStart()),
								positions.positionAt(record.getContentEnd())), replacement));
						}
					}
				}
			}
			first = end - 1;
		}
		return cancelled.getAsBoolean() ? new ArrayList<>() : edits;
	}

	private int[] widths(final List<CsvRecord> block) {
		final int count = block.getFirst().getCells().size();
		if (count < 2) {
			return null;
		}
		final int[] widths = new int[count];
		for (final CsvRecord record : block) {
			if (!record.isValid() || record.getKind() == Kind.OTHER) {
				return null;
			}
			if (record.getKind() != Kind.HEADER && record.getKind() != Kind.DATA) {
				continue;
			}
			if (record.isMultiline() || record.getCells().size() != count) {
				return null;
			}
			for (int i = 0; i < count; i++) {
				final int length = record.getCells().get(i).trim().length();
				if (length > MAX_ALIGNMENT_WIDTH) {
					return null;
				}
				widths[i] = Math.max(widths[i], length);
			}
		}
		return widths;
	}

	private String align(final List<String> cells, final int[] widths) {
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < cells.size(); i++) {
			final String cell = cells.get(i).trim();
			result.append(cell);
			if (i + 1 < cells.size()) {
				result.append(" ".repeat(widths[i] - cell.length())).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return result.toString();
	}
}
