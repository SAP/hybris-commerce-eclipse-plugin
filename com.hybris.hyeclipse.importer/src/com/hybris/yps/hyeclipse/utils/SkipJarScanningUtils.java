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
package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hybris.yps.hyeclipse.Activator;
//import com.hybris.yps.hyeclipse.ExtensionHolder;

/**
 * Created by Qiang Zeng on 26/06/2017.
 */
public class SkipJarScanningUtils {
	public static final String REGEX_TO_REPLACE = "org.apache.catalina.startup.TldConfig.jarsToSkip=[^\\#]*";
	public static final String REPLACEMENT_PREFIX = "org.apache.catalina.startup.TldConfig.jarsToSkip=tomcat7-websocket.jar";

	private static Set<String> excludedJarNamePrefixes = new HashSet<>();
	private static Set<String> excludedJarPaths = new HashSet<>();

	private static List<String> dirsToCheck = new ArrayList<>();

	static {
		dirsToCheck.add("lib");
		dirsToCheck.add("web/webroot/WEB-INF/lib");
		dirsToCheck.add("bin");

		excludedJarNamePrefixes.add("jstl");
		excludedJarNamePrefixes.add("z");
		excludedJarNamePrefixes.add("spring-web");
		excludedJarNamePrefixes.add("webFragmentCore");
		excludedJarNamePrefixes.add("spring-security-taglibs-");
		excludedJarNamePrefixes.add("spring-security-web-");
		excludedJarNamePrefixes.add("displaytag");

		excludedJarPaths.add("cockpit/lib");
	}

	private static Activator plugin = Activator.getDefault();
	private static final boolean DEBUG = plugin.isDebugging();
	
	private SkipJarScanningUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static void skipJarScanning(File platformHome) {
		
		Path catalinaPropPath = Paths.get(platformHome.getAbsolutePath(), "..", "..", "config", "tomcat", "conf", "catalina.properties");

		File catalinaFile = catalinaPropPath.toFile();
		List<String> jarNameList = new ArrayList<>();
		if (!catalinaFile.exists()) {
			throw new IllegalStateException(String.format("file %s doesn't exist.", catalinaPropPath));
		}
		try {
//			Set<ExtensionHolder> exts = plugin.getAllExtensionsForPlatform();
//			for (ExtensionHolder ext : exts) {
//				File extDir = new File(ext.getPath());
//				skipJarsByExtension(jarNameList, extDir);
//			}

			Collections.sort(jarNameList);

			StringBuilder sb = new StringBuilder();
			for (String jar : jarNameList) {
				sb.append(",\\\\").append(System.lineSeparator()).append(jar);
			}
			sb.append(System.lineSeparator()).append(System.lineSeparator());

			String content = new String(Files.readAllBytes(Paths.get(catalinaFile.getAbsolutePath())));

			Pattern regex = Pattern.compile(REGEX_TO_REPLACE, Pattern.DOTALL);
			Matcher regexMatcher = regex.matcher(content);
			if (regexMatcher.find()) {
				content = regexMatcher.replaceAll(REPLACEMENT_PREFIX + sb.toString());
			}

			try (PrintWriter writer = new PrintWriter(catalinaFile, StandardCharsets.UTF_8.name())) {
				writer.println(content);	
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to access the server.xml file at: " + catalinaPropPath);
		}
	}

	private static void skipJarsByExtension(List<String> jarNames, File extDir) {

		try {
			for (String dirToCheck : dirsToCheck) {
				File libDir = Paths.get(extDir.getAbsolutePath(), dirToCheck).toFile();
				includeJarToSkip(jarNames, libDir);
			}
		} catch (Exception e) {
			Activator.logError("could not skip jar file", e);
		}
	}

	private static void includeJarToSkip(List<String> jarNameSet, File dir) {
		for (String excludedPath : excludedJarPaths) {
			if (dir.getAbsolutePath().endsWith(excludedPath)) {
				Activator.log(">>>>> Not to skip:  " + dir.getAbsolutePath());
				return;
			}
		}
		
		if (dir.exists() && dir.listFiles() != null) {
			for (File file : dir.listFiles()) {
				if (file.getName().endsWith("jar") && !jarNameSet.contains(file.getName())) {
					boolean addToSkip = true;
					for (String excludedPrefix : excludedJarNamePrefixes) {
						if (file.getName().startsWith(excludedPrefix)) {
							if (DEBUG)
								Activator.log(">>>>> Not to skip:  " + file.getName());
							addToSkip = false;
							break;
						}
					}
					if (addToSkip) {
						jarNameSet.add(file.getName());
					}
				}
			}
		}
	}
}
