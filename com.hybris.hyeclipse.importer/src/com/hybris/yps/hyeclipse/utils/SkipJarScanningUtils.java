package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.io.PrintWriter;
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
import com.hybris.yps.hyeclipse.ExtensionHolder;

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
	private static final boolean debug = plugin.isDebugging();
	
	private SkipJarScanningUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Set<ExtensionHolder> getAllExtensionsForPlatform(String platformHome) {

		Set<ExtensionHolder> allExtensions = plugin.getAllExtensionsForPlatform(platformHome);
		return allExtensions;
	}

	public static void skipJarScanning(File platformHome) {
		
		Path catalinaPropPath = Paths.get(platformHome.getAbsolutePath(), "..", "..", "config", "tomcat", "conf", "catalina.properties");

		File catalinaFile = catalinaPropPath.toFile();
		List<String> jarNameList = new ArrayList<>();
		if (catalinaFile.exists()) {
			try {
				Set<ExtensionHolder> exts = getAllExtensionsForPlatform(platformHome.getAbsolutePath());
				if (debug)
					Activator.log("" + exts);
				for (ExtensionHolder ext : exts) {
					File extDir = new File(ext.getPath());
					skipJarsByExtension(jarNameList, extDir);
				}

				Collections.sort(jarNameList);

				StringBuilder sb = new StringBuilder();
				for (String jar : jarNameList) {
					if (debug)
						Activator.log(jar);
					sb.append(",\\\\").append("\n").append(jar);
				}
				sb.append("\n").append("\n");

				String content = new String(Files.readAllBytes(Paths.get(catalinaFile.getAbsolutePath())));

				Pattern regex = Pattern.compile(REGEX_TO_REPLACE, Pattern.DOTALL);
				Matcher regexMatcher = regex.matcher(content);
				if (regexMatcher.find()) {
					if (debug)
						Activator.log(regexMatcher.group(0));
					content = regexMatcher.replaceAll(REPLACEMENT_PREFIX + sb.toString());
				}

				try (PrintWriter writer = new PrintWriter(catalinaFile, "UTF-8")) {
					writer.println(content);	
				}
			} catch (Exception e) {
				throw new IllegalStateException("Failed to access the server.xml file at: " + catalinaPropPath);
			}
		} else {
			throw new IllegalStateException(catalinaPropPath + " doesn't exist.");
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
							if (debug)
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
