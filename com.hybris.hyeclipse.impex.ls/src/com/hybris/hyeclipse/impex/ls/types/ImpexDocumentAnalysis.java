/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.ls.types;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.CsvRecord;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.Kind;
import com.hybris.hyeclipse.impex.ls.format.TextPositions;

/** Source-only outline and confirmed CSV quote diagnostics, independent of the Commerce index. */
public final class ImpexDocumentAnalysis {

	private static final int MAX_SYMBOLS = 1000;
	private static final int MAX_DIAGNOSTICS = 100;

	/**
	 * @param text document snapshot
	 * @param cancelled cancellation predicate
	 * @return lexical header/macro declarations in source order, not inferred runtime scopes
	 */
	public List<DocumentSymbol> symbols(final String text, final BooleanSupplier cancelled) {
		final List<DocumentSymbol> symbols = new ArrayList<>();
		if (text.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH || cancelled.getAsBoolean()) {
			return symbols;
		}
		final List<CsvRecord> records;
		try {
			records = new ImpexCsvScanner().scan(text, cancelled);
		} catch (final IllegalArgumentException e) {
			return symbols;
		}
		final TextPositions positions = new TextPositions(text);
		for (final CsvRecord record : records) {
			if (cancelled.getAsBoolean()) {
				return new ArrayList<>();
			}
			if (!record.isValid()) {
				continue;
			}
			final String raw = record.getCells().getFirst();
			int start = 0;
			while (start < raw.length() && raw.charAt(start) <= ' ') {
				start++;
			}
			if (start < raw.length() && raw.charAt(start) == '"') {
				start++;
			}
			final String first = raw.substring(start);
			final int base = record.getStart() + start;
			final Range range = new Range(positions.positionAt(record.getStart()),
					positions.positionAt(record.getContentEnd()));
			if (record.getKind() == Kind.MACRO) {
				final Matcher macro = ImpexLanguageFeatures.MACRO.matcher(first);
				if (macro.find()) {
					symbols.add(new DocumentSymbol(macro.group(1), SymbolKind.Variable, range,
							new Range(positions.positionAt(base + macro.start(1)),
									positions.positionAt(base + macro.end(1)))));
				}
			} else if (record.getKind() == Kind.HEADER && !record.isMultiline()) {
				final Matcher mode = ImpexLanguageFeatures.MODE.matcher(first);
				if (mode.find()) {
					int end = mode.end();
					while (end < first.length() && ImpexLanguageFeatures.nameChar(first.charAt(end), true)) {
						end++;
					}
					if (end > mode.end()) {
						symbols.add(new DocumentSymbol(mode.group(1) + " " + first.substring(mode.end(), end), //$NON-NLS-1$
								SymbolKind.Class, range, new Range(positions.positionAt(base + mode.end()),
										positions.positionAt(base + end))));
					}
				}
			}
			if (symbols.size() >= MAX_SYMBOLS) {
				break;
			}
		}
		return cancelled.getAsBoolean() ? new ArrayList<>() : symbols;
	}

	/**
	 * @param text document snapshot
	 * @param cancelled cancellation predicate
	 * @return confirmed quote errors only; no type, macro, modifier, or import-result validation
	 */
	public List<Diagnostic> diagnostics(final String text, final BooleanSupplier cancelled) {
		final List<Diagnostic> diagnostics = new ArrayList<>();
		if (text.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH || cancelled.getAsBoolean()) {
			return diagnostics;
		}
		final List<CsvRecord> records;
		try {
			records = new ImpexCsvScanner().scan(text, cancelled);
		} catch (final IllegalArgumentException e) {
			return diagnostics;
		}
		final TextPositions positions = new TextPositions(text);
		for (final CsvRecord record : records) {
			if (cancelled.getAsBoolean()) {
				return new ArrayList<>();
			}
			if (record.getTextAfterQuote() >= 0) {
				add(diagnostics, text, positions, record.getTextAfterQuote(), "impex.textAfterQuote", Messages.textAfterQuote); //$NON-NLS-1$
			}
			if (record.getUnclosedQuote() >= 0 && diagnostics.size() < MAX_DIAGNOSTICS) {
				add(diagnostics, text, positions, record.getUnclosedQuote(), "impex.unclosedQuote", Messages.unclosedQuote); //$NON-NLS-1$
			}
			if (diagnostics.size() >= MAX_DIAGNOSTICS) {
				break;
			}
		}
		return cancelled.getAsBoolean() ? new ArrayList<>() : diagnostics;
	}

	private void add(final List<Diagnostic> diagnostics, final String text, final TextPositions positions,
			final int offset, final String code, final String message) {
		final int end = offset + Character.charCount(text.codePointAt(offset));
		final Diagnostic diagnostic = new Diagnostic(new Range(positions.positionAt(offset), positions.positionAt(end)),
				message, DiagnosticSeverity.Error, "ImpEx"); //$NON-NLS-1$
		diagnostic.setCode(code);
		diagnostics.add(diagnostic);
	}
}
