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
import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4j.Position;

/** UTF-16 positions with CR, LF, and CRLF support for an immutable document snapshot. */
public final class TextPositions {

	private final String text;
	private final int[] starts;

	/** @param text immutable document text */
	public TextPositions(final String text) {
		this.text = text;
		final List<Integer> offsets = new ArrayList<>();
		offsets.add(0);
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\r') {
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				offsets.add(i + 1);
			} else if (text.charAt(i) == '\n') {
				offsets.add(i + 1);
			}
		}
		starts = offsets.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * @param position a valid UTF-16 position
	 * @return its source offset
	 */
	public int offsetAt(final Position position) {
		if (position.getLine() < 0 || position.getLine() >= starts.length || position.getCharacter() < 0) {
			throw new IllegalArgumentException("Invalid document position"); //$NON-NLS-1$
		}
		final int start = starts[position.getLine()];
		int end = position.getLine() + 1 < starts.length ? starts[position.getLine() + 1] : text.length();
		while (end > start && (text.charAt(end - 1) == '\r' || text.charAt(end - 1) == '\n')) {
			end--;
		}
		if (position.getCharacter() > end - start) {
			throw new IllegalArgumentException("Document position extends past the line"); //$NON-NLS-1$
		}
		return start + position.getCharacter();
	}

	/**
	 * @param offset a source offset
	 * @return its UTF-16 position
	 */
	public Position positionAt(final int offset) {
		if (offset < 0 || offset > text.length()) {
			throw new IllegalArgumentException("Invalid document offset"); //$NON-NLS-1$
		}
		final int index = Arrays.binarySearch(starts, offset);
		final int line = index >= 0 ? index : -index - 2;
		return new Position(line, offset - starts[line]);
	}
}
