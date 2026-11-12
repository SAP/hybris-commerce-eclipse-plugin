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
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.hybris.hyeclipse.commons.Activator;

/** Workspace-local snapshots, keyed by canonical platform home and loaded on first access. */
public final class SystemConfigRepository {

	private static final String STORAGE_DIRECTORY = "system-configs";
	private static final String FILE_SUFFIX = ".xml";
	private final Path directory;
	private final Map<String, SystemConfig> cache = new HashMap<>();
	private final Map<String, SystemConfig> imports = new HashMap<>();
	private final SystemConfigXmlSerializer serializer = new SystemConfigXmlSerializer();

	/**
	 * @param stateLocation owning bundle's workspace state directory
	 */
	public SystemConfigRepository(final File stateLocation) {
		directory = stateLocation.toPath().resolve(STORAGE_DIRECTORY);
	}

	/**
	 * Performs disk I/O on a cache miss; UI callers should use a background Job.
	 * @param platformHome platform identity
	 * @return snapshot, or empty if never imported or the platform no longer exists
	 * @throws CoreException if the stored snapshot cannot be read
	 */
	public synchronized Optional<SystemConfig> find(final File platformHome) throws CoreException {
		try {
			final String key = platformHome.getCanonicalPath();
			if (!platformHome.isDirectory()) {
				cache.remove(key);
				return Optional.empty();
			}
			final SystemConfig importing = imports.get(key);
			if (importing != null) {
				return Optional.of(importing);
			}
			final SystemConfig cached = cache.get(key);
			if (cached != null) {
				return Optional.of(cached);
			}
			final Path file = fileFor(key);
			if (Files.notExists(file)) {
				return Optional.empty();
			}
			final SystemConfig config;
			try (final InputStream input = Files.newInputStream(file)) {
				config = serializer.read(input);
			}
			if (!key.equals(config.getPlatformHome().getCanonicalPath())) {
				throw new IOException("Stored platform identity does not match the requested platform");
			}
			cache.put(key, config);
			return Optional.of(config);
		} catch (IOException e) {
			throw failure("Could not load system configuration; rerun the platform import", e);
		}
	}

	/**
	 * Makes a provisional snapshot available to all plugins during project import, without persisting it.
	 * Closing the scope restores committed lookups; save the snapshot before closing to commit it.
	 * @param config validated snapshot being imported
	 * @return scope that must be closed on both success and failure
	 * @throws CoreException if the platform identity cannot be resolved
	 * @throws IllegalStateException if the same platform is already being imported
	 */
	public synchronized ImportSnapshot beginImport(final SystemConfig config) throws CoreException {
		config.validate();
		try {
			final String key = config.getPlatformHome().getCanonicalPath();
			if (imports.containsKey(key)) {
				throw new IllegalStateException("This platform is already being imported");
			}
			imports.put(key, config);
			return new ImportSnapshot(key);
		} catch (IOException e) {
			throw failure("Could not resolve the imported platform", e);
		}
	}

	/** Scoped provisional configuration; never writes to disk on close. */
	public final class ImportSnapshot implements AutoCloseable {
		private final String key;
		private boolean closed;

		private ImportSnapshot(final String key) {
			this.key = key;
		}

		@Override
		public void close() {
			synchronized (SystemConfigRepository.this) {
				if (!closed) {
					imports.remove(key);
					closed = true;
				}
			}
		}
	}

	/**
	 * Persists before publishing, so a failed write leaves the previous snapshot available.
	 * @param config validated import snapshot
	 * @throws CoreException if persistence fails
	 */
	public synchronized void save(final SystemConfig config) throws CoreException {
		config.validate();
		try {
			final String key = config.getPlatformHome().getCanonicalPath();
			Files.createDirectories(directory);
			final Path temporary = Files.createTempFile(directory, "system-config-", ".tmp");
			try {
				try (final OutputStream output = Files.newOutputStream(temporary)) {
					serializer.write(config, output);
				}
				try {
					Files.move(temporary, fileFor(key), StandardCopyOption.ATOMIC_MOVE,
							StandardCopyOption.REPLACE_EXISTING);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(temporary, fileFor(key), StandardCopyOption.REPLACE_EXISTING);
				}
				cache.put(key, config);
			} finally {
				Files.deleteIfExists(temporary);
			}
		} catch (IOException e) {
			throw failure("Could not persist system configuration", e);
		}
	}

	/**
	 * @param platformHome platform whose persisted snapshot should be removed
	 * @throws CoreException if removal fails
	 */
	public synchronized void remove(final File platformHome) throws CoreException {
		try {
			final String key = platformHome.getCanonicalPath();
			Files.deleteIfExists(fileFor(key));
			cache.remove(key);
		} catch (IOException e) {
			throw failure("Could not remove system configuration", e);
		}
	}

	private Path fileFor(final String key) {
		return directory.resolve(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)) + FILE_SUFFIX);
	}

	private static CoreException failure(final String message, final IOException cause) {
		return new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, cause));
	}
}
