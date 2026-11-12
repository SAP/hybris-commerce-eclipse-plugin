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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Workspace-local, versioned model snapshots; save failures never replace the last good index. */
public final class TypeSystemRepository {
	private static final String SCHEMA_VERSION = "1";
	private static final int MAX_ENTRIES = 1000000;
	private final Path directory;
	private final Map<String, TypeSystemSnapshot> snapshots = new ConcurrentHashMap<>();

	/** @param stateLocation owning plugin's workspace state directory */
	public TypeSystemRepository(final File stateLocation) {
		directory = stateLocation.toPath().resolve("type-system");
	}

	/**
	 * @param platformHome platform identity
	 * @return cached snapshot without disk I/O
	 */
	public TypeSystemSnapshot peek(final File platformHome) {
		return snapshots.getOrDefault(platformHome.getAbsolutePath(), TypeSystemSnapshot.empty());
	}

	/**
	 * @param platformHome platform identity
	 * @return persisted snapshot, or empty when none exists
	 * @throws IOException if the snapshot is corrupt or unsupported
	 */
	public synchronized Optional<TypeSystemSnapshot> load(final File platformHome) throws IOException {
		final String key = platformHome.getCanonicalPath();
		final Path file = fileFor(key);
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		final Properties xml = new Properties();
		try (final InputStream input = Files.newInputStream(file)) {
			xml.loadFromXML(input);
		}
		if (!SCHEMA_VERSION.equals(xml.getProperty("schema.version")) || !key.equals(xml.getProperty("platform"))) {
			throw new IOException("Unsupported or mismatched type-system snapshot");
		}
		try {
			final List<YType> types = new ArrayList<>();
			final int count = count(xml, "types");
			for (int i = 0; i < count; i++) {
				final String prefix = "type." + i + ".";
				final List<YAttributeDescriptor> attributes = new ArrayList<>();
				final int attributeCount = count(xml, prefix + "attributes");
				for (int j = 0; j < attributeCount; j++) {
					final String attribute = prefix + "attribute." + j + ".";
					attributes.add(new YAttributeDescriptor(required(xml, attribute + "qualifier"),
							required(xml, attribute + "javaType"), required(xml, attribute + "declaringClass"),
							required(xml, attribute + "source"), Integer.parseInt(required(xml, attribute + "line"))));
				}
				types.add(new YType(required(xml, prefix + "code"), required(xml, prefix + "class"),
						required(xml, prefix + "superClass"), required(xml, prefix + "source"),
						Integer.parseInt(required(xml, prefix + "line")), attributes));
			}
			final TypeSystemSnapshot snapshot = new TypeSystemSnapshot(types);
			publish(platformHome, key, snapshot);
			return Optional.of(snapshot);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid type-system snapshot", e);
		}
	}

	/**
	 * @param platformHome platform identity
	 * @param snapshot complete replacement index
	 * @throws IOException if persistence fails
	 */
	public synchronized void save(final File platformHome, final TypeSystemSnapshot snapshot) throws IOException {
		final String key = platformHome.getCanonicalPath();
		final Properties xml = new Properties();
		xml.setProperty("schema.version", SCHEMA_VERSION);
		xml.setProperty("platform", key);
		xml.setProperty("types", Integer.toString(snapshot.getTypes().size()));
		int i = 0;
		for (final YType type : snapshot.getTypes()) {
			final String prefix = "type." + i++ + ".";
			xml.setProperty(prefix + "code", type.getCode());
			xml.setProperty(prefix + "class", type.getJavaClassName());
			xml.setProperty(prefix + "superClass", type.getSuperClassName());
			xml.setProperty(prefix + "source", type.getSourcePath());
			xml.setProperty(prefix + "line", Integer.toString(type.getSourceLine()));
			xml.setProperty(prefix + "attributes", Integer.toString(type.getAttributes().size()));
			int j = 0;
			for (final YAttributeDescriptor attribute : type.getAttributes()) {
				final String item = prefix + "attribute." + j++ + ".";
				xml.setProperty(item + "qualifier", attribute.getQualifier());
				xml.setProperty(item + "javaType", attribute.getJavaType());
				xml.setProperty(item + "declaringClass", attribute.getDeclaringClass());
				xml.setProperty(item + "source", attribute.getSourcePath());
				xml.setProperty(item + "line", Integer.toString(attribute.getSourceLine()));
			}
		}
		Files.createDirectories(directory);
		final Path temporary = Files.createTempFile(directory, "types-", ".tmp");
		try {
			try (final OutputStream output = Files.newOutputStream(temporary)) {
				xml.storeToXML(output, "Commerce generated model types", StandardCharsets.UTF_8);
			}
			try {
				Files.move(temporary, fileFor(key), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, fileFor(key), StandardCopyOption.REPLACE_EXISTING);
			}
			publish(platformHome, key, snapshot);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void publish(final File home, final String key, final TypeSystemSnapshot snapshot) {
		snapshots.put(key, snapshot);
		snapshots.put(home.getAbsolutePath(), snapshot);
	}

	private Path fileFor(final String key) {
		return directory.resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + ".xml");
	}

	private static String required(final Properties xml, final String key) {
		final String value = xml.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException("Missing snapshot field: " + key);
		}
		return value;
	}

	private static int count(final Properties xml, final String key) {
		final int value = Integer.parseInt(required(xml, key));
		if (value < 0 || value > MAX_ENTRIES) {
			throw new IllegalArgumentException("Invalid snapshot entry count");
		}
		return value;
	}
}
