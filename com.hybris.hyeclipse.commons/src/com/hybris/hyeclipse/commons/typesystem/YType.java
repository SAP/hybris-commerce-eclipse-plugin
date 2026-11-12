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
package com.hybris.hyeclipse.commons.typesystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable generated-model type with its effective, inherited attributes. */
public final class YType {
	private final String code;
	private final String javaClassName;
	private final String superClassName;
	private final String sourcePath;
	private final int sourceLine;
	private final Map<String, YAttributeDescriptor> attributes;

	/**
	 * @param code model's own _TYPECODE
	 * @param javaClassName fully qualified model class
	 * @param superClassName fully qualified superclass, or empty
	 * @param sourcePath workspace source path
	 * @param sourceLine one-based type declaration line
	 * @param attributes effective attributes, including inherited ones
	 */
	public YType(final String code, final String javaClassName, final String superClassName,
			final String sourcePath, final int sourceLine, final Collection<YAttributeDescriptor> attributes) {
		this.code = code;
		this.javaClassName = javaClassName;
		this.superClassName = superClassName;
		this.sourcePath = sourcePath;
		this.sourceLine = sourceLine;
		final Map<String, YAttributeDescriptor> byName = new LinkedHashMap<>();
		for (final YAttributeDescriptor attribute : attributes) {
			byName.put(attribute.getQualifier(), attribute);
		}
		this.attributes = Collections.unmodifiableMap(byName);
	}

	/** @return Commerce type code */
	public String getCode() {
		return code;
	}

	/** @return fully qualified model class */
	public String getJavaClassName() {
		return javaClassName;
	}

	/** @return fully qualified superclass */
	public String getSuperClassName() {
		return superClassName;
	}

	/** @return workspace source path */
	public String getSourcePath() {
		return sourcePath;
	}

	/** @return one-based type declaration line */
	public int getSourceLine() {
		return sourceLine;
	}

	/** @return read-only effective attributes */
	public Collection<YAttributeDescriptor> getAttributes() {
		return attributes.values();
	}

	/** @return attribute names in deterministic order */
	public List<String> getAttributeNames() {
		return Collections.unmodifiableList(new ArrayList<>(attributes.keySet()));
	}

	/**
	 * @param qualifier attribute name
	 * @return attribute, or null when absent
	 */
	public YAttributeDescriptor getAttribute(final String qualifier) {
		return attributes.get(qualifier);
	}
}
