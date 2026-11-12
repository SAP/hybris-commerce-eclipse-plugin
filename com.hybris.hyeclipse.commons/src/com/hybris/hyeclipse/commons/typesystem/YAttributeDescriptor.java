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

/** Immutable source-derived attribute, independent of the Commerce bootstrap library. */
public final class YAttributeDescriptor {
	private final String qualifier;
	private final String javaType;
	private final String declaringClass;
	private final String sourcePath;
	private final int sourceLine;

	/**
	 * @param qualifier Accessor qualifier
	 * @param javaType accessor Java type, including generic arguments
	 * @param declaringClass fully qualified declaring model class
	 * @param sourcePath workspace source path
	 * @param sourceLine one-based declaration line
	 */
	public YAttributeDescriptor(final String qualifier, final String javaType, final String declaringClass,
			final String sourcePath, final int sourceLine) {
		this.qualifier = qualifier;
		this.javaType = javaType;
		this.declaringClass = declaringClass;
		this.sourcePath = sourcePath;
		this.sourceLine = sourceLine;
	}

	/** @return attribute name */
	public String getQualifier() {
		return qualifier;
	}

	/** @return accessor Java type */
	public String getJavaType() {
		return javaType;
	}

	/** @return declaring model's fully qualified name */
	public String getDeclaringClass() {
		return declaringClass;
	}

	/** @return workspace source path */
	public String getSourcePath() {
		return sourcePath;
	}

	/** @return one-based declaration line */
	public int getSourceLine() {
		return sourceLine;
	}
}
