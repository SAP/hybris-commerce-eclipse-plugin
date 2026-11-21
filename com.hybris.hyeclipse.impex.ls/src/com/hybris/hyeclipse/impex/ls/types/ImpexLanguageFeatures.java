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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.osgi.util.NLS;

import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.CsvRecord;
import com.hybris.hyeclipse.impex.ls.format.ImpexCsvScanner.Kind;
import com.hybris.hyeclipse.impex.ls.format.TextPositions;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog.Attribute;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog.Type;

/** Conservative header-context assistance using indexed metadata, not runtime import semantics. */
public final class ImpexLanguageFeatures {

	private static final int MAX_PROPOSALS = 200;
	private static final int MAX_HOVER_TEXT = 2048;
	static final Pattern MODE = Pattern.compile("(?i)^\\s*(INSERT_UPDATE|INSERT|UPDATE|REMOVE)(?:\\s+|$)"); //$NON-NLS-1$
	static final Pattern MACRO = Pattern.compile("^\\s*(\\$[\\w-]+)\\s*="); //$NON-NLS-1$
	private static final List<String> OPERATIONS = Arrays.asList("INSERT", "INSERT_UPDATE", "REMOVE", "UPDATE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final List<String> HEADER_MODIFIERS = Arrays.asList("batchmode", "cacheUnique", "processor", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"parallel", "ignoreKeyCase", "unlessExisting", "impex.legacy.mode", "sld.enabled", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"disable.interceptor.types", "disable.interceptor.beans", "disable.UniqueAttributesValidator.for.types", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"testMode"); //$NON-NLS-1$
	private static final List<String> COLUMN_MODIFIERS = Arrays.asList("unique", "allownull", "ignorenull", "default", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"virtual", "lang", "translator", "cellDecorator", "dateformat", "numberformat", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"collection-delimiter", "path-delimiter", "key2value-delimiter", "map-delimiter", "mode", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"query-chunk-size", "forceWrite", "pos", "alias", "sourceSystemId", "pkFromExternalImportKey"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private static final List<String> BOOLEAN_MODIFIERS = Arrays.asList("unique", "allownull", "ignorenull", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"virtual", "forceWrite", "pkFromExternalImportKey", "batchmode", "cacheUnique", "parallel", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"ignoreKeyCase", "unlessExisting", "impex.legacy.mode", "sld.enabled", "testMode"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private enum Area { NONE, OPERATION, TYPE, ATTRIBUTE, MODIFIER, VALUE, MACRO }

	/** Provider-specific declaration identity; the host resolves accessibility and URI. */
	public static final class SourceTarget {
		private final String path;
		private final int line;

		private SourceTarget(final String path, final int line) {
			this.path = path;
			this.line = line;
		}
		/** @return provider-specific source path */
		public String getPath() { return path; }
		/** @return one-based declaration line */
		public int getLine() { return line; }
	}

	private static final class Token {
		private final int start;
		private final int end;
		private final String text;
		private final String prefix;

		private Token(final String source, final int offset, final boolean dots) {
			int from = offset;
			int to = offset;
			while (from > 0 && nameChar(source.charAt(from - 1), dots)) {
				from--;
			}
			while (to < source.length() && nameChar(source.charAt(to), dots)) {
				to++;
			}
			start = from;
			end = to;
			text = source.substring(from, to);
			prefix = source.substring(from, offset);
		}
	}

	private static final class Context {
		private Area area = Area.NONE;
		private Token token;
		private Type type;
		private boolean headerModifier;
		private String modifier;
	}

	private static final class Scope {
		private final Type base;
		private Type current;
		private String lastName;

		private Scope(final Type type) {
			base = type;
			current = type;
		}
	}

	/**
	 * @param text open document snapshot
	 * @param position caret position in UTF-16 units
	 * @param catalog document-scoped indexed types
	 * @param cancelled cancellation/stale-document predicate
	 * @return context-specific, token-replacing proposals; incomplete when the limit is reached
	 */
	public CompletionList complete(final String text, final Position position, final TypeCatalog catalog,
			final BooleanSupplier cancelled) {
		final CompletionList result = emptyCompletion();
		if (text.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH || cancelled.getAsBoolean()) {
			return result;
		}
		final TextPositions positions = new TextPositions(text);
		final int offset;
		final List<CsvRecord> records;
		try {
			offset = positions.offsetAt(position);
			records = new ImpexCsvScanner().scan(text, cancelled);
		} catch (final IllegalArgumentException e) {
			return result;
		}
		final Context context = context(text, offset, records, catalog);
		switch (context.area) {
		case TYPE:
			for (final Type type : catalog.getTypes()) {
				if (cancelled.getAsBoolean()) {
					return emptyCompletion();
				}
				if (!offer(result, type.getCode(), type.getJavaClass(), CompletionItemKind.Class, context.token, positions)) {
					break;
				}
			}
			break;
		case ATTRIBUTE:
			if (context.type != null) {
				for (final Attribute attribute : context.type.getAttributes()) {
					if (!offer(result, attribute.getName(), attribute.getJavaType(), CompletionItemKind.Property,
							context.token, positions)) {
						break;
					}
				}
			}
			break;
		case OPERATION:
			words(result, OPERATIONS, Messages.operation, CompletionItemKind.Keyword, context.token, positions);
			break;
		case MODIFIER:
			words(result, context.headerModifier ? HEADER_MODIFIERS : COLUMN_MODIFIERS,
					context.headerModifier ? Messages.headerModifier : Messages.columnModifier,
					CompletionItemKind.Property, context.token, positions);
			break;
		case VALUE:
			if ("mode".equalsIgnoreCase(context.modifier)) { //$NON-NLS-1$
				words(result, Arrays.asList("append", "remove", "merge"), Messages.modifierValue, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						CompletionItemKind.Value, context.token, positions);
			} else if (BOOLEAN_MODIFIERS.stream().anyMatch(name -> name.equalsIgnoreCase(context.modifier))) {
				words(result, Arrays.asList("true", "false"), Messages.modifierValue, //$NON-NLS-1$ //$NON-NLS-2$
						CompletionItemKind.Value, context.token, positions);
			}
			break;
		case MACRO:
			final Set<String> names = new TreeSet<>();
			for (final CsvRecord record : records) {
				if (record.getEnd() <= offset && record.getContentEnd() < offset && record.getKind() == Kind.MACRO) {
					final Matcher definition = MACRO.matcher(ImpexCsvScanner.valueOf(record.getCells().getFirst()));
					if (definition.find()) {
						names.add(definition.group(1));
					}
				}
			}
			words(result, new ArrayList<>(names), Messages.macro, CompletionItemKind.Variable, context.token, positions);
			break;
		default:
			break;
		}
		return cancelled.getAsBoolean() ? emptyCompletion() : result;
	}

	/**
	 * @param text open document snapshot
	 * @param position requested UTF-16 position
	 * @param catalog document-scoped metadata
	 * @return indexed type/attribute declaration, or null when the context cannot be resolved safely
	 */
	public SourceTarget definition(final String text, final Position position, final TypeCatalog catalog) {
		if (text.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH) {
			return null;
		}
		final Context context;
		try {
			context = context(text, new TextPositions(text).offsetAt(position), new ImpexCsvScanner().scan(text), catalog);
		} catch (final IllegalArgumentException e) {
			return null;
		}
		if (context.area == Area.TYPE) {
			final Type type = catalog.getType(context.token.text);
			return type == null ? null : new SourceTarget(type.getSourcePath(), type.getSourceLine());
		}
		if (context.area == Area.ATTRIBUTE && context.type != null) {
			final Attribute attribute = context.type.getAttribute(context.token.text);
			return attribute == null ? null : new SourceTarget(attribute.getSourcePath(), attribute.getSourceLine());
		}
		return null;
	}

	/**
	 * @param text open document snapshot
	 * @param position requested UTF-16 position
	 * @param catalog document-scoped metadata
	 * @param cancelled cancellation/stale-document predicate
	 * @return plain-text type, attribute, or macro information; null for unknown contexts
	 */
	public Hover hover(final String text, final Position position, final TypeCatalog catalog,
			final BooleanSupplier cancelled) {
		if (text.length() > ImpexCsvScanner.MAX_DOCUMENT_LENGTH || cancelled.getAsBoolean()) {
			return null;
		}
		final TextPositions positions = new TextPositions(text);
		final int offset;
		final List<CsvRecord> records;
		try {
			offset = positions.offsetAt(position);
			records = new ImpexCsvScanner().scan(text, cancelled);
		} catch (final IllegalArgumentException e) {
			return null;
		}
		final Context context = context(text, offset, records, catalog);
		String description = null;
		if (context.area == Area.TYPE) {
			final Type type = catalog.getType(context.token.text);
			if (type != null) {
				description = NLS.bind(Messages.typeHover, type.getCode(), type.getJavaClass());
			}
		} else if (context.area == Area.ATTRIBUTE && context.type != null) {
			final Attribute attribute = context.type.getAttribute(context.token.text);
			if (attribute != null) {
				description = NLS.bind(Messages.attributeHover,
						new Object[] {context.type.getCode(), attribute.getName(), attribute.getJavaType()});
			}
		} else if (context.area == Area.MACRO) {
			for (final CsvRecord record : records) {
				if (cancelled.getAsBoolean()) {
					return null;
				}
				if (record.isValid() && record.getKind() == Kind.MACRO) {
					final Matcher macro = MACRO.matcher(ImpexCsvScanner.valueOf(record.getCells().getFirst()));
					final boolean earlier = record.getEnd() <= offset && record.getContentEnd() < offset;
					final boolean declaration = context.token.start >= record.getStart()
							&& context.token.end <= record.getStart() + record.getCells().getFirst().indexOf('=');
					if ((earlier || declaration) && macro.find() && macro.group(1).equals(context.token.text)) {
						final int end = Math.min(record.getContentEnd(), record.getStart() + MAX_HOVER_TEXT);
						final String definition = text.substring(record.getStart(), end)
								+ (end < record.getContentEnd() ? Messages.truncated : ""); //$NON-NLS-1$
						description = NLS.bind(Messages.macroHover, context.token.text, definition);
					}
				}
			}
		}
		if (description == null || cancelled.getAsBoolean()) {
			return null;
		}
		if (description.length() > MAX_HOVER_TEXT) {
			int end = MAX_HOVER_TEXT;
			if (Character.isHighSurrogate(description.charAt(end - 1)) && Character.isLowSurrogate(description.charAt(end))) {
				end--;
			}
			description = description.substring(0, end) + Messages.truncated;
		}
		return new Hover(new MarkupContent(MarkupKind.PLAINTEXT, description),
				new Range(positions.positionAt(context.token.start), positions.positionAt(context.token.end)));
	}

	/** @return an empty, complete proposal list */
	public static CompletionList emptyCompletion() {
		return new CompletionList(false, new ArrayList<>());
	}

	private Context context(final String text, final int offset, final List<CsvRecord> records,
			final TypeCatalog catalog) {
		final Context result = new Context();
		result.token = new Token(text, offset, false);
		CsvRecord record = null;
		for (final CsvRecord candidate : records) {
			if (candidate.getStart() <= offset && offset <= candidate.getContentEnd()) {
				record = candidate;
				break;
			}
		}
		if (record != null && (record.getKind() == Kind.COMMENT || record.getKind() == Kind.SCRIPT)) {
			return result;
		}
		if (result.token.text.startsWith("$")) { //$NON-NLS-1$
			result.area = Area.MACRO;
			return result;
		}
		if (record == null) {
			result.area = Area.OPERATION;
			return result;
		}
		if (record.isMultiline() || record.getKind() == Kind.DATA || record.getKind() == Kind.MACRO) {
			return result;
		}
		final String first = record.getCells().getFirst();
		final Matcher mode = MODE.matcher(first);
		if (!mode.find()) {
			if (first.trim().matches("[A-Za-z_]*")) { //$NON-NLS-1$
				result.area = Area.OPERATION;
			}
			return result;
		}
		if (offset <= record.getStart() + mode.end(1)) {
			result.area = Area.OPERATION;
			return result;
		}
		int typeEnd = mode.end();
		while (typeEnd < first.length() && nameChar(first.charAt(typeEnd), false)) {
			typeEnd++;
		}
		final Type headerType = catalog.getType(first.substring(mode.end(), typeEnd));
		int cell = 0;
		int cellStart = record.getStart();
		while (cell + 1 < record.getCells().size()
				&& offset > cellStart + record.getCells().get(cell).length()) {
			cellStart += record.getCells().get(cell).length() + 1;
			cell++;
		}
		if (cell == 0 && offset <= record.getStart() + typeEnd) {
			result.area = Area.TYPE;
			return result;
		}
		final String prefix = text.substring(cellStart, offset);
		final int element = modifierElement(prefix);
		if (element >= 0) {
			result.token = new Token(text, offset, true);
			final int equals = prefix.indexOf('=', element);
			result.area = equals >= 0 ? Area.VALUE : Area.MODIFIER;
			result.headerModifier = cell == 0;
			result.modifier = equals >= 0 ? prefix.substring(element, equals).trim() : null;
			return result;
		}
		if (cell > 0 && (prefix.indexOf('[') < 0 || prefix.lastIndexOf(']') >= prefix.lastIndexOf('['))) {
			if (record.getCells().get(cell).trim().startsWith("@") //$NON-NLS-1$
					|| record.getCells().get(cell).trim().startsWith("&")) { //$NON-NLS-1$
				return result;
			}
			result.area = Area.ATTRIBUTE;
			result.type = referenceContext(catalog, headerType, text.substring(cellStart, result.token.start));
		}
		return result;
	}

	private int modifierElement(final String prefix) {
		int element = -1;
		char quote = 0;
		for (int i = 0; i < prefix.length(); i++) {
			final char c = prefix.charAt(i);
			if (quote != 0) {
				if (c == quote) {
					quote = 0;
				}
			} else if (c == '\'' || c == '"') {
				quote = c;
			} else if (c == '[') {
				element = i + 1;
			} else if (c == ']') {
				element = -1;
			} else if (c == ',' && element >= 0) {
				element = i + 1;
			}
		}
		return quote == 0 ? element : -1;
	}

	private Type referenceContext(final TypeCatalog catalog, final Type headerType, final String prefix) {
		final Deque<Scope> scopes = new ArrayDeque<>();
		scopes.push(new Scope(headerType));
		int brackets = 0;
		char quote = 0;
		for (int i = 0; i < prefix.length(); i++) {
			final char c = prefix.charAt(i);
			if (quote != 0) {
				if (c == quote) {
					quote = 0;
				}
				continue;
			}
			if (c == '\'' || c == '"') {
				quote = c;
				continue;
			}
			if (c == '[') {
				brackets++;
			} else if (c == ']') {
				brackets--;
			} else if (brackets == 0) {
				final Scope scope = scopes.peek();
				if (c == '(') {
					scopes.push(new Scope(reference(catalog, scope.current, scope.lastName)));
				} else if (c == ')') {
					if (scopes.size() == 1) {
						return null;
					}
					scopes.pop();
				} else if (c == ',') {
					scope.current = scope.base;
					scope.lastName = null;
				} else if (c == '.') {
					final Attribute attribute = scope.current == null ? null : scope.current.getAttribute(scope.lastName);
					scope.current = attribute != null ? catalog.referenceType(attribute.getJavaType())
							: catalog.getType(scope.lastName);
					scope.lastName = null;
				} else if (nameChar(c, false)) {
					final int start = i;
					while (i + 1 < prefix.length() && nameChar(prefix.charAt(i + 1), false)) {
						i++;
					}
					scope.lastName = prefix.substring(start, i + 1);
				}
			}
		}
		return quote == 0 && brackets == 0 ? scopes.peek().current : null;
	}

	private Type reference(final TypeCatalog catalog, final Type type, final String name) {
		final Attribute attribute = type == null || name == null ? null : type.getAttribute(name);
		return attribute == null ? null : catalog.referenceType(attribute.getJavaType());
	}

	static boolean nameChar(final char c, final boolean dots) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '$' || (dots && c == '.');
	}

	private void words(final CompletionList result, final List<String> words, final String detail,
			final CompletionItemKind kind, final Token token, final TextPositions positions) {
		for (final String word : new TreeSet<>(words)) {
			if (!offer(result, word, detail, kind, token, positions)) {
				break;
			}
		}
	}

	private boolean offer(final CompletionList result, final String label, final String detail,
			final CompletionItemKind kind, final Token token, final TextPositions positions) {
		if (!label.regionMatches(true, 0, token.prefix, 0, token.prefix.length())) {
			return true;
		}
		if (result.getItems().size() >= MAX_PROPOSALS) {
			result.setIsIncomplete(true);
			return false;
		}
		final CompletionItem item = new CompletionItem(label);
		item.setDetail(detail);
		item.setKind(kind);
		item.setTextEdit(Either.forLeft(new TextEdit(new Range(positions.positionAt(token.start),
				positions.positionAt(token.end)), label)));
		result.getItems().add(item);
		return true;
	}
}
