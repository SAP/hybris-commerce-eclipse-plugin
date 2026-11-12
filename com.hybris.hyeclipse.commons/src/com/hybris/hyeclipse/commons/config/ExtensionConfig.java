/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.commons.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.hybris.hyeclipse.commons.Constants;

/**
 * Representation of an extension configuration parsed from Ant build logs.
 */
public class ExtensionConfig {

	private final String name;
	private final String version;
	private final String flags;
	private final File extensionDirectory;
	private final boolean platformExtension;
	private final boolean autoRequired;
	private final boolean lazyLoaded;
	private final boolean coreModule;
	private final boolean webModule;
	private final boolean hasItemsXml;
	private final boolean hasBeansXml;
	private final boolean deprecated;
	private final List<String> dependencies;
	private final List<String> requiredBy;
	private final String packageRoot;
	private final boolean hmcModule;
	private final boolean backofficeModule;

	public ExtensionConfig(final String name, final String version, final File extensionDirectory,
			final String flags, final boolean deprecated, final List<String> dependencies,
			final List<String> requiredBy) {
		this(name, version, extensionDirectory, flags, deprecated, dependencies, requiredBy, null, false, false);
	}

	ExtensionConfig(final String name, final String version, final File extensionDirectory,
			final String flags, final boolean deprecated, final List<String> dependencies,
			final List<String> requiredBy, final String packageRoot, final boolean hmcModule,
			final boolean backofficeModule) {
		this.name = name;
		this.version = version;
		this.extensionDirectory = extensionDirectory;
		this.deprecated = deprecated;
		this.packageRoot = packageRoot;
		this.hmcModule = hmcModule;
		this.backofficeModule = backofficeModule;
		this.dependencies = dependencies != null ? Collections.unmodifiableList(new ArrayList<>(dependencies))
				: Collections.emptyList();
		this.requiredBy = requiredBy != null ? Collections.unmodifiableList(new ArrayList<>(requiredBy))
				: Collections.emptyList();

		final String f = flags != null ? flags : "";
		this.flags = f;
		this.platformExtension = f.indexOf('p') >= 0;
		this.autoRequired = f.indexOf('*') >= 0;
		this.lazyLoaded = f.indexOf('?') >= 0;
		this.coreModule = f.indexOf('c') >= 0;
		this.webModule = f.indexOf('w') >= 0;
		this.hasItemsXml = f.indexOf('i') >= 0;
		this.hasBeansXml = f.indexOf('b') >= 0;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	/** @return the original Ant extension flags */
	public String getFlags() {
		return flags;
	}

	/** @return core-module package root, or null when none is declared */
	public String getPackageRoot() {
		return packageRoot;
	}

	/** @return whether extensioninfo.xml declares an HMC module */
	public boolean isHmcModule() {
		return hmcModule;
	}

	/** @return whether the extension is a Backoffice module */
	public boolean isBackofficeModule() {
		return backofficeModule;
	}

	ExtensionConfig readMetadata() throws IOException {
		if (extensionDirectory == null) {
			return this;
		}
		final File descriptor = new File(extensionDirectory, Constants.EXTENSION_INFO_XML);
		if (!descriptor.isFile()) {
			return this;
		}
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		String root = null;
		boolean hmc = false;
		boolean backoffice = false;
		try (final InputStream input = Files.newInputStream(descriptor.toPath())) {
			final XMLStreamReader reader = factory.createXMLStreamReader(input);
			try {
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT) {
						continue;
					}
					final String element = reader.getLocalName();
					if ("coremodule".equals(element)) {
						root = reader.getAttributeValue(null, "packageroot");
					} else if ("hmcmodule".equals(element)) {
						hmc = true;
					} else if ("meta".equals(element)
							&& "backoffice-module".equals(reader.getAttributeValue(null, "key"))) {
						backoffice = Boolean.parseBoolean(reader.getAttributeValue(null, "value"));
					}
				}
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException("Could not read extension metadata: " + descriptor, e);
		}
		return new ExtensionConfig(name, version, extensionDirectory, flags, deprecated, dependencies, requiredBy,
				root, hmc, backoffice);
	}

	public File getExtensionDirectory() {
		return extensionDirectory;
	}

	public boolean isPlatformExtension() {
		return platformExtension;
	}

	public boolean isAutoRequired() {
		return autoRequired;
	}

	public boolean isLazyLoaded() {
		return lazyLoaded;
	}

	public boolean isCoreModule() {
		return coreModule;
	}

	public boolean isWebModule() {
		return webModule;
	}

	public boolean hasItemsXml() {
		return hasItemsXml;
	}

	public boolean hasBeansXml() {
		return hasBeansXml;
	}

	public boolean isDeprecated() {
		return deprecated;
	}

	public List<String> getDependencies() {
		return dependencies;
	}

	public List<String> getRequiredBy() {
		return requiredBy;
	}

	@Override
	public String toString() {
		return "ExtensionConfig [name=" + name + ", version=" + version + ", extensionDirectory="
				+ extensionDirectory + ", coreModule=" + coreModule + ", webModule=" + webModule + "]";
	}
}
