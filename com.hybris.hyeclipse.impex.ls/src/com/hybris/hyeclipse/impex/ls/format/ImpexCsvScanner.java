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
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Scans ImpEx cells without interpreting macros, headers, or executable code. */
public final class ImpexCsvScanner {

	public static final int MAX_DOCUMENT_LENGTH = 4 * 1024 * 1024;
	public static final int MAX_COLUMNS = 512;
	private static final int MAX_RECORDS = 100_000;
	private static final int CANCELLATION_INTERVAL = 4096;
	private static final Pattern HEADER = Pattern.compile("(?i)^(INSERT_UPDATE|INSERT|UPDATE|REMOVE)\\s+\\S.*"); //$NON-NLS-1$

	public enum Kind {
		HEADER, DATA, COMMENT, SCRIPT, MACRO, BLANK, OTHER
	}

	/** One logical CSV record, retaining its original source offsets and raw cells. */
	public static final class CsvRecord {
		private final int start;
		private final int contentEnd;
		private final int end;
		private final List<String> cells;
		private final boolean valid;
		private final boolean multiline;
		private final Kind kind;
		private final int textAfterQuote;
		private final int unclosedQuote;

		private CsvRecord(final int start, final int contentEnd, final int end, final List<String> cells,
				final boolean valid, final boolean multiline, final int textAfterQuote, final int unclosedQuote) {
			this.start = start;
			this.contentEnd = contentEnd;
			this.end = end;
			this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
			this.valid = valid;
			this.multiline = multiline;
			this.textAfterQuote = textAfterQuote;
			this.unclosedQuote = unclosedQuote;
			final String first = valueOf(cells.get(0));
			if (first.startsWith("#%")) { //$NON-NLS-1$
				kind = Kind.SCRIPT;
			} else if (first.startsWith("#")) { //$NON-NLS-1$
				kind = Kind.COMMENT;
			} else if (first.startsWith("$") && first.contains("=")) { //$NON-NLS-1$ //$NON-NLS-2$
				kind = Kind.MACRO;
			} else if (HEADER.matcher(first).matches()) {
				kind = Kind.HEADER;
			} else if (first.isEmpty() && cells.size() > 1) {
				kind = Kind.DATA;
			} else if (first.isEmpty() && cells.size() == 1) {
				kind = Kind.BLANK;
			} else {
				kind = Kind.OTHER;
			}
		}

		/** @return inclusive source start */
		public int getStart() { return start; }
		/** @return exclusive content end, before the final line delimiter */
		public int getContentEnd() { return contentEnd; }
		/** @return exclusive record end, including its final line delimiter */
		public int getEnd() { return end; }
		/** @return immutable raw cells, including empty and trailing cells */
		public List<String> getCells() { return cells; }
		/** @return whether quotes and text following closing quotes are valid */
		public boolean isValid() { return valid; }
		/** @return whether quoted or backslash-continued lines were joined */
		public boolean isMultiline() { return multiline; }
		/** @return lexical record kind */
		public Kind getKind() { return kind; }
		/** @return first offending character after a closing quote, or -1 when absent */
		public int getTextAfterQuote() { return textAfterQuote; }
		/** @return opening quote still unmatched at EOF, or -1 when absent */
		public int getUnclosedQuote() { return unclosedQuote; }
	}

	/**
	 * Extracts a cell value using doubled quotes and ImpEx edge trimming.
	 * Intended for valid, single-line cells; it does not expand macros.
	 * @param raw the original cell text
	 * @return the decoded, trimmed cell value
	 */
	public static String valueOf(final String raw) {
		final String trimmed = raw.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
			return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return trimmed;
	}

	/**
	 * @param source document text
	 * @return logical records preserving source boundaries
	 */
	public List<CsvRecord> scan(final String source) {
		return scan(source, () -> false);
	}

	/**
	 * Scans a bounded document. A quote opens quoting only before a cell has started;
	 * a doubled quote is literal, and a backslash never escapes a quote.
	 * @param source document text
	 * @param cancelled cancellation predicate
	 * @return logical records, or an empty list on cancellation
	 * @throws IllegalArgumentException when document/record/column limits are exceeded
	 */
	public List<CsvRecord> scan(final String source, final BooleanSupplier cancelled) {
		if (source.length() > MAX_DOCUMENT_LENGTH) {
			throw new IllegalArgumentException("ImpEx document exceeds the scanning limit"); //$NON-NLS-1$
		}
		final List<CsvRecord> records = new ArrayList<>();
		List<String> cells = new ArrayList<>();
		int recordStart = 0;
		int cellStart = 0;
		boolean quoted = false;
		boolean cellStarted = false;
		boolean cellEnded = false;
		boolean valid = true;
		boolean multiline = false;
		int quoteStart = -1;
		int textAfterQuote = -1;
		for (int i = 0; i < source.length(); i++) {
			if (i % CANCELLATION_INTERVAL == 0 && cancelled.getAsBoolean()) {
				return new ArrayList<>();
			}
			final char c = source.charAt(i);
			if (c == '\r' || c == '\n') {
				final int delimiterLength = c == '\r' && i + 1 < source.length()
						&& source.charAt(i + 1) == '\n' ? 2 : 1;
				if (quoted || (i > recordStart && source.charAt(i - 1) == '\\')) {
					multiline = true;
					i += delimiterLength - 1;
					continue;
				}
				cells.add(source.substring(cellStart, i));
				add(records, new CsvRecord(recordStart, i, i + delimiterLength, cells, valid, multiline,
						textAfterQuote, -1));
				i += delimiterLength - 1;
				recordStart = i + 1;
				cellStart = recordStart;
				cells = new ArrayList<>();
				cellStarted = false;
				cellEnded = false;
				valid = true;
				multiline = false;
				quoteStart = -1;
				textAfterQuote = -1;
			} else if (quoted) {
				if (c == '"') {
					if (i + 1 < source.length() && source.charAt(i + 1) == '"') {
						i++;
					} else {
						quoted = false;
						quoteStart = -1;
						cellEnded = true;
					}
				}
			} else if (c == ';') {
				cells.add(source.substring(cellStart, i));
				if (cells.size() >= MAX_COLUMNS) {
					throw new IllegalArgumentException("ImpEx record exceeds the column limit"); //$NON-NLS-1$
				}
				cellStart = i + 1;
				cellStarted = false;
				cellEnded = false;
			} else if (c == '"' && !cellStarted) {
				quoted = true;
				quoteStart = i;
				cellStarted = true;
			} else if (!Character.isWhitespace(c)) {
				if (cellEnded) {
					valid = false;
					if (textAfterQuote < 0) {
						textAfterQuote = i;
					}
				}
				cellStarted = true;
			}
		}
		if (recordStart < source.length()) {
			cells.add(source.substring(cellStart));
			// A final continuation marker without a following line is ambiguous to format.
			valid = valid && !quoted && !source.endsWith("\\"); //$NON-NLS-1$
			add(records, new CsvRecord(recordStart, source.length(), source.length(), cells, valid, multiline,
					textAfterQuote, quoted ? quoteStart : -1));
		}
		return records;
	}

	private void add(final List<CsvRecord> records, final CsvRecord record) {
		if (records.size() >= MAX_RECORDS) {
			throw new IllegalArgumentException("ImpEx document exceeds the record limit"); //$NON-NLS-1$
		}
		records.add(record);
	}
}
