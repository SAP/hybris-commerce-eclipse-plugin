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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Standalone clone of {@code de.hybris.bootstrap.config.SystemConfig} populated
 * from Ant dependency update build logs.
 */
public class SystemConfig {

	public static final String PLATFORM_HOME = "platformhome";
	public static final String PROPERTY_BIN_DIR = "HYBRIS_BIN_DIR";
	public static final String PROPERTY_CONFIG_DIR = "HYBRIS_CONFIG_DIR";
	public static final String PROPERTY_BOOTSTRAP_BIN_DIR = "HYBRIS_BOOTSTRAP_BIN_DIR";
	public static final String PROPERTY_ROLES_DIR = "HYBRIS_ROLES_DIR";
	public static final String PROPERTY_DATA_DIR = "HYBRIS_DATA_DIR";
	public static final String PROPERTY_LOG_DIR = "HYBRIS_LOG_DIR";
	public static final String PROPERTY_TEMP_DIR = "HYBRIS_TEMP_DIR";
	public static final String PROPERTY_ACTIVE_ROLE = "ACTIVE_ROLE";
	public static final String PROPERTY_ACTIVE_ROLE_INSTANCE = "ACTIVE_ROLE_INSTANCE";
	public static final String PROPERTY_EXTENSION_FILE = "HYBRIS_EXTENSIONS_XML";
	public static final Double PLATFORM_V_6_3 = 6.3d;
	public static final Double PLATFORM_V_18_11 = 18.11d;
	public static final Double PLATFORM_V_22_11 = 22.11d;
	public static final double JVM8_VERSION = 5.6d;
	public static final double JVM11_VERSION = 1811d;
	public static final Pattern platformVersionPattern = Pattern.compile("[0-9]*\\.[0-9]*");


	private static final Pattern PROP_LINE_PATTERN = Pattern.compile("^\\s*\\[echo\\]\\s+([^:]+):\\s*(.*)$");

	private static final Pattern EXT_LINE_PATTERN = Pattern.compile("^\\s*\\[echo\\]\\s+"
			+ "(?:(@deprecated)\\s+)?"
			+ "([a-zA-Z0-9_\\-]+)"
			+ "(?:->(?:\\(([^)]+)\\)|([a-zA-Z0-9_\\-]+)))?"
			+ "(?:\\s*<-(?:\\?-|-)(?:\\(([^)]+)\\)|([a-zA-Z0-9_\\-]+)))?"
			+ "\\s+([^\\s\\[]+)"
			+ "\\s+\\[([^\\]]+)\\]"
			+ "(?:\\s+path:(\\S+))?");

	private static final Pattern DEPRECATED_EXTS_PATTERN = Pattern
			.compile("Deprecated extensions:\\s*\\[(.*)\\]");

	private File platformHome;
	private File configDir;
	private File binDir;
	private File dataDir;
	private File logDir;
	private File tempDir;
	private File bootstrapBinDir;
	private File rolesDir;
	private File extensionFileUsed;

	private String activeRole;
	private String activeRoleInstance;
	private String platformVersion;
	private String osFamily;
	private String javaPlatform;
	private String javaCompiler;
	private String buildTarget;
	private String compilationMode;
	private String antVersion;
	private String antMemorySettings;

	private final Map<String, String> props = new HashMap<>();
	private final List<String> deprecatedExtensions = new ArrayList<>();
	private final List<ExtensionConfig> extensionsInOrder = new ArrayList<>();
	private final Map<String, ExtensionConfig> extensionsByName = new LinkedHashMap<>();

	public SystemConfig() {
		// default constructor
	}

	/**
	 * Parses build log output lines into a {@link SystemConfig} instance.
	 *
	 * @param logLines the Ant build log lines
	 * @return populated {@link SystemConfig}
	 */
	public static SystemConfig parse(final Collection<String> logLines) {
		final SystemConfig config = new SystemConfig();
		if (logLines == null || logLines.isEmpty()) {
			return config;
		}

		for (final String line : logLines) {
			if (line == null) {
				continue;
			}
			config.parseLine(line.trim());
		}
		config.initDerivedPaths();
		return config;
	}

	private void parseLine(final String line) {
		final Matcher depMatcher = DEPRECATED_EXTS_PATTERN.matcher(line);
		if (depMatcher.find()) {
			final String[] names = depMatcher.group(1).split(",\\s*");
			for (final String name : names) {
				if (StringUtils.isNotBlank(name)) {
					deprecatedExtensions.add(name.trim());
				}
			}
			return;
		}

		final Matcher extMatcher = EXT_LINE_PATTERN.matcher(line);
		if (extMatcher.matches()) {
			parseExtension(extMatcher);
			return;
		}

		final Matcher propMatcher = PROP_LINE_PATTERN.matcher(line);
		if (propMatcher.matches()) {
			final String key = propMatcher.group(1).trim();
			final String value = propMatcher.group(2).trim();
			parseProperty(key, value);
		}
	}

	private void parseProperty(final String label, final String value) {
		switch (label) {
			case "Platform home":
			case "hybris Platform directory":
				this.platformHome = resolveFile(value);
				props.put(PLATFORM_HOME, value);
				break;
			case "Config folder":
				this.configDir = resolveFile(value);
				props.put(PROPERTY_CONFIG_DIR, value);
				break;
			case "Bin folder":
				this.binDir = resolveFile(value);
				props.put(PROPERTY_BIN_DIR, value);
				break;
			case "Data folder":
				this.dataDir = resolveFile(value);
				props.put(PROPERTY_DATA_DIR, value);
				break;
			case "Log folder":
				this.logDir = resolveFile(value);
				props.put(PROPERTY_LOG_DIR, value);
				break;
			case "Temp folder":
				this.tempDir = resolveFile(value);
				props.put(PROPERTY_TEMP_DIR, value);
				break;
			case "Boostrap bin folder":
			case "Bootstrap bin folder":
				this.bootstrapBinDir = resolveFile(value);
				props.put(PROPERTY_BOOTSTRAP_BIN_DIR, value);
				break;
			case "Roles folder":
				this.rolesDir = resolveFile(value);
				props.put(PROPERTY_ROLES_DIR, value);
				break;
			case "Extension file used":
				this.extensionFileUsed = resolveFile(value);
				props.put(PROPERTY_EXTENSION_FILE, value);
				break;
			case "hybris Platform version":
				this.platformVersion = value;
				props.put("hybris.platform.version", value);
				break;
			case "OS family":
				this.osFamily = value;
				props.put("os.family", value);
				break;
			case "Java platform":
				this.javaPlatform = value;
				props.put("java.platform", value);
				break;
			case "Java compiler":
				this.javaCompiler = value;
				props.put("java.compiler", value);
				break;
			case "Build target":
				this.buildTarget = value;
				props.put("build.target", value);
				break;
			case "Compilation mode":
				this.compilationMode = value;
				props.put("compilation.mode", value);
				break;
			case "Ant version":
				this.antVersion = value;
				props.put("ant.version", value);
				break;
			case "Ant memory settings":
				this.antMemorySettings = value;
				props.put("ant.opts", value);
				break;
			case "Active role":
				this.activeRole = value;
				props.put(PROPERTY_ACTIVE_ROLE, value);
				break;
			case "Active role instance":
				this.activeRoleInstance = value;
				props.put(PROPERTY_ACTIVE_ROLE_INSTANCE, value);
				break;
			default:
				props.put(label, value);
				break;
		}
	}

	private void parseExtension(final Matcher matcher) {
		final boolean isDeprecated = matcher.group(1) != null;
		final String name = matcher.group(2);
		final String depsList = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
		final String reqByList = matcher.group(5) != null ? matcher.group(5) : matcher.group(6);
		final String version = matcher.group(7);
		final String flags = matcher.group(8);
		final String pathStr = matcher.group(9);

		final List<String> deps = depsList != null ? Arrays.asList(depsList.split(",")) : Collections.emptyList();
		final List<String> reqBy = reqByList != null ? Arrays.asList(reqByList.split(",")) : Collections.emptyList();

		File extDir = null;
		if (StringUtils.isNotBlank(pathStr)) {
			extDir = resolveFile(pathStr);
		} else if (platformHome != null && flags != null && flags.indexOf('p') >= 0) {
			extDir = new File(new File(platformHome, "ext"), name);
		}

		final ExtensionConfig ext = new ExtensionConfig(name, version, extDir, flags, isDeprecated, deps, reqBy);
		extensionsInOrder.add(ext);
		extensionsByName.put(name, ext);
	}

	private void initDerivedPaths() {
		if (platformHome != null) {
			final File hybrisHome = platformHome.getParentFile().getParentFile();
			if (configDir == null) {
				configDir = new File(hybrisHome, "config");
			}
			if (binDir == null) {
				binDir = platformHome.getParentFile();
			}
			if (dataDir == null) {
				dataDir = new File(hybrisHome, "data");
			}
			if (logDir == null) {
				logDir = new File(hybrisHome, "log");
			}
			if (tempDir == null) {
				tempDir = new File(hybrisHome, "temp/hybris");
			}
			if (bootstrapBinDir == null) {
				bootstrapBinDir = new File(platformHome, "bootstrap/bin");
			}
		}
	}

	/**
	 * Rejects incomplete Ant output before workspace projects are modified.
	 *
	 * @throws IllegalArgumentException if mandatory configuration is absent
	 */
	public void validate() {
		if (platformHome == null || configDir == null || StringUtils.isBlank(platformVersion)
				|| extensionsInOrder.isEmpty()) {
			throw new IllegalArgumentException("Incomplete system configuration; rerun the platform import");
		}
	}

	/**
	 * Fetches extension descriptor data needed by other plugins before publishing the snapshot.
	 * @return a new snapshot including package roots and legacy module metadata
	 * @throws IOException if an existing descriptor cannot be parsed
	 */
	public SystemConfig withExtensionMetadata() throws IOException {
		final List<ExtensionConfig> extensions = new ArrayList<>();
		for (final ExtensionConfig extension : extensionsInOrder) {
			extensions.add(extension.readMetadata());
		}
		return fromState(getState(), props, deprecatedExtensions, extensions);
	}

	// Explicit persistence fields keep the file format independent of Java serialization and Ant log syntax.
	Map<String, String> getState() {
		final Map<String, String> state = new LinkedHashMap<>();
		putState(state, "Platform home", platformHome);
		putState(state, "Config folder", configDir);
		putState(state, "Bin folder", binDir);
		putState(state, "Data folder", dataDir);
		putState(state, "Log folder", logDir);
		putState(state, "Temp folder", tempDir);
		putState(state, "Bootstrap bin folder", bootstrapBinDir);
		putState(state, "Roles folder", rolesDir);
		putState(state, "Extension file used", extensionFileUsed);
		putState(state, "Active role", activeRole);
		putState(state, "Active role instance", activeRoleInstance);
		putState(state, "hybris Platform version", platformVersion);
		putState(state, "OS family", osFamily);
		putState(state, "Java platform", javaPlatform);
		putState(state, "Java compiler", javaCompiler);
		putState(state, "Build target", buildTarget);
		putState(state, "Compilation mode", compilationMode);
		putState(state, "Ant version", antVersion);
		putState(state, "Ant memory settings", antMemorySettings);
		return state;
	}

	private static void putState(final Map<String, String> state, final String key, final Object value) {
		if (value != null) {
			state.put(key, value.toString());
		}
	}

	static SystemConfig fromState(final Map<String, String> state, final Map<String, String> properties,
			final List<String> deprecated, final List<ExtensionConfig> extensions) {
		final SystemConfig config = new SystemConfig();
		for (final Map.Entry<String, String> entry : state.entrySet()) {
			config.parseProperty(entry.getKey(), entry.getValue());
		}
		config.props.clear();
		config.props.putAll(properties);
		config.deprecatedExtensions.addAll(deprecated);
		config.extensionsInOrder.addAll(extensions);
		for (final ExtensionConfig extension : extensions) {
			if (config.extensionsByName.put(extension.getName(), extension) != null) {
				throw new IllegalArgumentException("Duplicate extension: " + extension.getName());
			}
		}
		return config;
	}

	private File resolveFile(final String path) {
		if (StringUtils.isBlank(path)) {
			return null;
		}
		final File f = new File(path.trim());
		try {
			return f.getCanonicalFile();
		} catch (IOException e) {
			return f.getAbsoluteFile();
		}
	}

	public File getPlatformHome() {
		return platformHome;
	}

	public File getConfigDir() {
		return configDir;
	}

	public File getBinDir() {
		return binDir;
	}

	public File getDataDir() {
		return dataDir;
	}

	public File getLogDir() {
		return logDir;
	}

	public File getTempDir() {
		return tempDir;
	}

	public File getBootstrapBinDir() {
		return bootstrapBinDir;
	}

	public File getRolesDir() {
		return rolesDir;
	}

	public File getExtensionFileUsed() {
		return extensionFileUsed;
	}

	public String getActiveRole() {
		return activeRole;
	}

	public String getActiveRoleInstance() {
		return activeRoleInstance;
	}

	public String getPlatformVersion() {
		return platformVersion;
	}

	public String getOsFamily() {
		return osFamily;
	}

	public String getJavaPlatform() {
		return javaPlatform;
	}

	public String getJavaCompiler() {
		return javaCompiler;
	}

	public String getBuildTarget() {
		return buildTarget;
	}

	public String getCompilationMode() {
		return compilationMode;
	}

	public String getAntVersion() {
		return antVersion;
	}

	public String getAntMemorySettings() {
		return antMemorySettings;
	}

	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(props);
	}

	public List<String> getDeprecatedExtensions() {
		return Collections.unmodifiableList(deprecatedExtensions);
	}

	public List<ExtensionConfig> getExtensionsInOrder() {
		return Collections.unmodifiableList(extensionsInOrder);
	}

	public ExtensionConfig getExtension(final String name) {
		return extensionsByName.get(name);
	}

	public String getDir(final String propertyName) {
		if (PLATFORM_HOME.equalsIgnoreCase(propertyName)) {
			return platformHome != null ? platformHome.getAbsolutePath() : null;
		}
		final String val = props.get(propertyName);
		return val != null ? replaceProperties(val) : null;
	}

	public File getFile(final String key, final String path, final boolean checkExists) {
		if (StringUtils.isBlank(path)) {
			throw new IllegalArgumentException("Property " + key + " is not set");
		}
		final File file = new File(path.trim());
		if (checkExists && !file.exists()) {
			throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
		}
		return file;
	}

	public String replaceProperties(final String path) {
		if (path == null) {
			return null;
		}
		String result = path;
		if (platformHome != null) {
			result = result.replace("${platformhome}", platformHome.getAbsolutePath());
		}
		for (final Map.Entry<String, String> entry : props.entrySet()) {
			result = result.replace("${" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}

	public static String replaceMacros(final String path, final String platformHome) {
		if (path == null) {
			return null;
		}
		return path.replace("${platformhome}", platformHome != null ? platformHome : "");
	}
}
