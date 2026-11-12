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
package com.hybris.hyeclipse.commons.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Versioned XML codec using the JDK properties format, without Java object serialization. */
public final class SystemConfigXmlSerializer {

	private static final String SCHEMA_VERSION = "1";
	private static final String SCHEMA_KEY = "schema.version";
	private static final String STATE_PREFIX = "state.";
	private static final String PROPERTY_PREFIX = "property.";
	private static final String EXTENSION_PREFIX = "extension.";
	private static final String DEPRECATED_PREFIX = "deprecated.";
	private static final String COUNT_SUFFIX = "count";
	private static final int MAX_COLLECTION_SIZE = 100000;

	/**
	 * Writes all effective fields and ordered extensions. The caller owns the stream.
	 * @param config snapshot to serialize
	 * @param output destination
	 * @throws IOException if writing fails
	 */
	public void write(final SystemConfig config, final OutputStream output) throws IOException {
		config.validate();
		final Properties xml = new Properties();
		xml.setProperty(SCHEMA_KEY, SCHEMA_VERSION);
		putMap(xml, STATE_PREFIX, config.getState());
		putMap(xml, PROPERTY_PREFIX, config.getProperties());
		putList(xml, DEPRECATED_PREFIX, config.getDeprecatedExtensions());
		final List<ExtensionConfig> extensions = config.getExtensionsInOrder();
		xml.setProperty(EXTENSION_PREFIX + COUNT_SUFFIX, Integer.toString(extensions.size()));
		for (int i = 0; i < extensions.size(); i++) {
			final ExtensionConfig extension = extensions.get(i);
			final String prefix = EXTENSION_PREFIX + i + ".";
			xml.setProperty(prefix + "name", extension.getName());
			put(xml, prefix + "version", extension.getVersion());
			put(xml, prefix + "directory", extension.getExtensionDirectory());
			xml.setProperty(prefix + "flags", extension.getFlags());
			xml.setProperty(prefix + "deprecated", Boolean.toString(extension.isDeprecated()));
			put(xml, prefix + "packageRoot", extension.getPackageRoot());
			xml.setProperty(prefix + "hmcModule", Boolean.toString(extension.isHmcModule()));
			xml.setProperty(prefix + "backofficeModule", Boolean.toString(extension.isBackofficeModule()));
			putList(xml, prefix + "dependencies.", extension.getDependencies());
			putList(xml, prefix + "requiredBy.", extension.getRequiredBy());
		}
		xml.storeToXML(output, "SAP Commerce workspace configuration", "UTF-8");
	}

	/**
	 * Reads a snapshot and rejects unsupported or incomplete data.
	 * @param input serialized configuration (closed by the JDK XML reader)
	 * @return restored snapshot
	 * @throws IOException if the XML, schema or configuration is invalid
	 */
	public SystemConfig read(final InputStream input) throws IOException {
		final Properties xml = new Properties();
		// The JDK reader uses only the built-in properties DTD, not arbitrary external entities.
		xml.loadFromXML(input);
		if (!SCHEMA_VERSION.equals(xml.getProperty(SCHEMA_KEY))) {
			throw new IOException("Unsupported system configuration schema version");
		}
		try {
			final List<ExtensionConfig> extensions = new ArrayList<>();
			final int count = count(xml, EXTENSION_PREFIX);
			for (int i = 0; i < count; i++) {
				final String prefix = EXTENSION_PREFIX + i + ".";
				final String directory = xml.getProperty(prefix + "directory");
				extensions.add(new ExtensionConfig(required(xml, prefix + "name"),
						xml.getProperty(prefix + "version"), directory != null ? new File(directory) : null,
						required(xml, prefix + "flags"), Boolean.parseBoolean(required(xml, prefix + "deprecated")),
						getList(xml, prefix + "dependencies."), getList(xml, prefix + "requiredBy."),
						xml.getProperty(prefix + "packageRoot"),
						Boolean.parseBoolean(required(xml, prefix + "hmcModule")),
						Boolean.parseBoolean(required(xml, prefix + "backofficeModule"))));
			}
			final SystemConfig config = SystemConfig.fromState(getMap(xml, STATE_PREFIX),
					getMap(xml, PROPERTY_PREFIX), getList(xml, DEPRECATED_PREFIX), extensions);
			config.validate();
			return config;
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid system configuration snapshot", e);
		}
	}

	private static void put(final Properties xml, final String key, final Object value) {
		if (value != null) {
			xml.setProperty(key, value.toString());
		}
	}

	private static void putMap(final Properties xml, final String prefix, final Map<String, String> values) {
		for (final Map.Entry<String, String> entry : values.entrySet()) {
			xml.setProperty(prefix + entry.getKey(), entry.getValue());
		}
	}

	private static Map<String, String> getMap(final Properties xml, final String prefix) {
		final Map<String, String> values = new HashMap<>();
		for (final String key : xml.stringPropertyNames()) {
			if (key.startsWith(prefix)) {
				values.put(key.substring(prefix.length()), xml.getProperty(key));
			}
		}
		return values;
	}

	private static void putList(final Properties xml, final String prefix, final List<String> values) {
		xml.setProperty(prefix + COUNT_SUFFIX, Integer.toString(values.size()));
		for (int i = 0; i < values.size(); i++) {
			xml.setProperty(prefix + i, values.get(i));
		}
	}

	private static List<String> getList(final Properties xml, final String prefix) {
		final List<String> values = new ArrayList<>();
		final int count = count(xml, prefix);
		for (int i = 0; i < count; i++) {
			values.add(required(xml, prefix + i));
		}
		return values;
	}

	private static int count(final Properties xml, final String prefix) {
		final int count = Integer.parseInt(required(xml, prefix + COUNT_SUFFIX));
		if (count < 0 || count > MAX_COLLECTION_SIZE) {
			throw new IllegalArgumentException("Invalid collection size");
		}
		return count;
	}

	private static String required(final Properties xml, final String key) {
		final String value = xml.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException("Missing configuration field: " + key);
		}
		return value;
	}
}
