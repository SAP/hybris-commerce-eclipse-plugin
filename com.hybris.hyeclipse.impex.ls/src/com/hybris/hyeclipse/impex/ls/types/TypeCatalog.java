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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable language metadata, independent of Eclipse workspace and UI classes. */
public final class TypeCatalog {

	private static final Pattern JAVA_NAME = Pattern.compile("[\\w.$]+"); //$NON-NLS-1$
	private static final TypeCatalog EMPTY = new TypeCatalog(new ArrayList<>());
	private final Map<String, Type> types;

	/** Source metadata for an attribute, including inherited declarations. */
	public static final class Attribute {
		private final String name;
		private final String javaType;
		private final String sourcePath;
		private final int sourceLine;

		/**
		 * @param name qualifier
		 * @param javaType accessor Java type
		 * @param sourcePath provider-specific source identity
		 * @param sourceLine one-based declaration line
		 */
		public Attribute(final String name, final String javaType, final String sourcePath, final int sourceLine) {
			this.name = name;
			this.javaType = javaType;
			this.sourcePath = sourcePath;
			this.sourceLine = sourceLine;
		}

		/** @return qualifier */
		public String getName() { return name; }
		/** @return accessor Java type */
		public String getJavaType() { return javaType; }
		/** @return provider-specific source identity */
		public String getSourcePath() { return sourcePath; }
		/** @return one-based declaration line */
		public int getSourceLine() { return sourceLine; }
	}

	/** Source-derived type and its effective attributes. */
	public static final class Type {
		private final String code;
		private final String javaClass;
		private final String sourcePath;
		private final int sourceLine;
		private final Map<String, Attribute> attributes;

		/**
		 * @param code Commerce type code
		 * @param javaClass fully qualified generated model class
		 * @param sourcePath provider-specific source identity
		 * @param sourceLine one-based declaration line
		 * @param attributes effective attributes, including inherited ones
		 */
		public Type(final String code, final String javaClass, final String sourcePath, final int sourceLine,
				final Collection<Attribute> attributes) {
			this.code = code;
			this.javaClass = javaClass;
			this.sourcePath = sourcePath;
			this.sourceLine = sourceLine;
			final Map<String, Attribute> values = new TreeMap<>();
			for (final Attribute attribute : attributes) {
				values.put(attribute.getName(), attribute);
			}
			this.attributes = Collections.unmodifiableMap(values);
		}

		/** @return Commerce type code */
		public String getCode() { return code; }
		/** @return generated model class */
		public String getJavaClass() { return javaClass; }
		/** @return provider-specific source identity */
		public String getSourcePath() { return sourcePath; }
		/** @return one-based declaration line */
		public int getSourceLine() { return sourceLine; }
		/** @return immutable attributes sorted by qualifier */
		public Collection<Attribute> getAttributes() { return attributes.values(); }

		/**
		 * @param name qualifier
		 * @return exact or unambiguous case-insensitive match
		 */
		public Attribute getAttribute(final String name) {
			if (name == null) {
				return null;
			}
			if (attributes.containsKey(name)) {
				return attributes.get(name);
			}
			Attribute found = null;
			for (final Attribute attribute : attributes.values()) {
				if (attribute.getName().equalsIgnoreCase(name)) {
					if (found != null) {
						return null;
					}
					found = attribute;
				}
			}
			return found;
		}
	}

	/** @param types source-derived types */
	public TypeCatalog(final Collection<Type> types) {
		final Map<String, Type> values = new TreeMap<>();
		for (final Type type : types) {
			if (values.put(type.getCode(), type) != null) {
				throw new IllegalArgumentException("Duplicate Commerce type code: " + type.getCode()); //$NON-NLS-1$
			}
		}
		this.types = Collections.unmodifiableMap(values);
	}

	/** @return shared empty catalog */
	public static TypeCatalog empty() { return EMPTY; }
	/** @return immutable types sorted by code */
	public Collection<Type> getTypes() { return types.values(); }

	/**
	 * @param code type code
	 * @return exact or unambiguous case-insensitive match
	 */
	public Type getType(final String code) {
		if (code == null) {
			return null;
		}
		if (types.containsKey(code)) {
			return types.get(code);
		}
		Type found = null;
		for (final Type type : types.values()) {
			if (type.getCode().equalsIgnoreCase(code)) {
				if (found != null) {
					return null;
				}
				found = type;
			}
		}
		return found;
	}

	/**
	 * Resolves a reference/collection accessor to one indexed model type, never by guessing a type code.
	 * @param javaType accessor type, possibly including generic arguments
	 * @return unique indexed model, or null for unknown/ambiguous types
	 */
	public Type referenceType(final String javaType) {
		if (javaType == null) {
			return null;
		}
		Type found = null;
		final Matcher names = JAVA_NAME.matcher(javaType);
		while (names.find()) {
			final String name = names.group();
			for (final Type type : types.values()) {
				final String model = type.getJavaClass();
				if (model.equals(name) || (name.indexOf('.') < 0
						&& model.substring(model.lastIndexOf('.') + 1).equals(name))) {
					if (found != null && found != type) {
						return null;
					}
					found = type;
				}
			}
		}
		return found;
	}
}
