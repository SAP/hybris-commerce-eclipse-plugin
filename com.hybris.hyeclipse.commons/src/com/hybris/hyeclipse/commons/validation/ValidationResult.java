/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.commons.validation;

import java.io.File;

/** Immutable validation finding with no dependency on the toolbox runtime. */
public final class ValidationResult {
	public enum State {
		PASS, FAIL, ERROR, SKIPPED
	}

	private final File file;
	private final int line;
	private final String ruleId;
	private final String priority;
	private final State state;
	private final String element;
	private final String description;

	/**
	 * @param file input file
	 * @param line one-based source line
	 * @param ruleId rule identifier
	 * @param priority H, M or L
	 * @param state evaluation state
	 * @param element affected type or qualifier
	 * @param description finding description
	 */
	public ValidationResult(final File file, final int line, final String ruleId, final String priority,
			final State state, final String element, final String description) {
		this.file = file;
		this.line = Math.max(1, line);
		this.ruleId = ruleId;
		this.priority = priority;
		this.state = state;
		this.element = element;
		this.description = description;
	}

	/** @return input file */
	public File getFile() {
		return file;
	}

	/** @return one-based source line */
	public int getLine() {
		return line;
	}

	/** @return rule identifier */
	public String getRuleId() {
		return ruleId;
	}

	/** @return H, M or L */
	public String getPriority() {
		return priority;
	}

	/** @return evaluation state */
	public State getState() {
		return state;
	}

	/** @return affected type/attribute */
	public String getElement() {
		return element;
	}

	/** @return human-readable finding */
	public String getDescription() {
		return description;
	}
}
