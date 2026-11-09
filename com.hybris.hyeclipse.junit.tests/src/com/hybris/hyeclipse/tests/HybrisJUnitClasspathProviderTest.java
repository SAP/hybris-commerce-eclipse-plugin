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
package com.hybris.hyeclipse.tests;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.tools.ToolProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.hyeclipse.junit.HybrisJUnitClasspathProvider;

public class HybrisJUnitClasspathProviderTest {

	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void collectsOutputsBeforeArchivesAndExcludesMissingFiles() throws Exception {
		final File extension = temporary.newFolder("extension with spaces");
		final File eclipseClasses = new File(extension, "eclipsebin/classes");
		final File classes = new File(extension, "classes");
		final File resources = new File(extension, "resources");
		Files.createDirectories(eclipseClasses.toPath());
		Files.createDirectories(classes.toPath());
		Files.createDirectories(resources.toPath());
		final File jar = new File(extension, "bin/exampleserver.jar");
		Files.createDirectories(jar.toPath().getParent());
		Files.createFile(jar.toPath());
		final File library = new File(extension, "lib/dependency.jar");
		Files.createDirectories(library.toPath().getParent());
		Files.createFile(library.toPath());
		Files.createFile(new File(extension, "lib/readme.txt").toPath());
		final TestProvider provider = new TestProvider(extension);
		Assert.assertEquals(Arrays.asList(eclipseClasses, classes, jar, resources, library),
				new ArrayList<>(provider.extensionFiles()));
	}

	@Test(timeout = 15000)
	public void resolvedClasspathLoadsClassesAndResourcesInModernJvm() throws Exception {
		final File extension = temporary.newFolder("runtime fixture");
		final File classes = new File(extension, "classes");
		final File resources = new File(extension, "resources");
		Files.createDirectories(classes.toPath());
		Files.createDirectories(resources.toPath());
		final File source = new File(extension, "ClasspathProbe.java");
		Files.writeString(source.toPath(), "public class ClasspathProbe { public static void main(String[] args) {"
				+ "if (ClasspathProbe.class.getResource(\"/fixture.txt\") == null) throw new AssertionError();"
				+ "}}", StandardCharsets.UTF_8);
		Files.writeString(new File(resources, "fixture.txt").toPath(), "available", StandardCharsets.UTF_8);
		Assert.assertNotNull("Test requires a JDK", ToolProvider.getSystemJavaCompiler());
		Assert.assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
				"-d", classes.getAbsolutePath(), source.getAbsolutePath()));
		final ILaunchConfigurationWorkingCopy configuration = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType("org.eclipse.jdt.junit.launchconfig").newInstance(null, "classpath-probe");
		final TestProvider provider = new TestProvider(extension);
		final IRuntimeClasspathEntry[] entries = provider.resolveClasspath(
				provider.computeUnresolvedClasspath(configuration), configuration);
		final List<String> locations = Arrays.stream(entries)
				.filter(entry -> entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES)
				.map(IRuntimeClasspathEntry::getLocation).collect(Collectors.toList());
		Assert.assertTrue(locations.contains(classes.getAbsolutePath()));
		Assert.assertTrue(locations.contains(resources.getAbsolutePath()));
		final Process process = new ProcessBuilder(new File(System.getProperty("java.home"), "bin/java").getPath(),
				"-cp", String.join(File.pathSeparator, locations), "ClasspathProbe").redirectErrorStream(true).start();
		try {
			Assert.assertTrue(process.waitFor(10, TimeUnit.SECONDS));
			Assert.assertEquals(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
					0, process.exitValue());
		} finally {
			process.destroyForcibly();
			process.getInputStream().close();
			process.getOutputStream().close();
			process.getErrorStream().close();
		}
	}

	private static class TestProvider extends HybrisJUnitClasspathProvider {
		private final File directory;

		TestProvider(final File directory) {
			this.directory = directory;
		}

		Set<File> extensionFiles() {
			final Set<File> files = new LinkedHashSet<>();
			addExtensionClasspath(files, directory, "example");
			return files;
		}

		@Override
		protected Collection<File> getCommerceClasspath(final ILaunchConfiguration configuration) throws CoreException {
			return extensionFiles();
		}
	}
}
