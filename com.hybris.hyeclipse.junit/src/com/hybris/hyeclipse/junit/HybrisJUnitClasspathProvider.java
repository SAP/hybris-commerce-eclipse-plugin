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
package com.hybris.hyeclipse.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardClasspathProvider;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.yps.hyeclipse.ExtensionHolder;

/** Supplies Commerce dependencies before JDT starts the test JVM. */
public class HybrisJUnitClasspathProvider extends StandardClasspathProvider {

	public static final String ID = "com.hybris.hyeclipse.junit.classpathProvider"; //$NON-NLS-1$
	public static final String TEST_KIND_ID = "com.hybris.junit.kind"; //$NON-NLS-1$

	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(final ILaunchConfiguration configuration)
			throws CoreException {
		final Set<IRuntimeClasspathEntry> entries = new LinkedHashSet<>(
				Arrays.asList(super.computeUnresolvedClasspath(configuration)));
		for (final File file : getCommerceClasspath(configuration)) {
			final IRuntimeClasspathEntry entry = JavaRuntime.newArchiveRuntimeClasspathEntry(
					Path.fromOSString(file.getAbsolutePath()));
			entry.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
			entries.add(entry);
		}
		return entries.toArray(new IRuntimeClasspathEntry[0]);
	}

	/**
	 * @param configuration selected test launch
	 * @return additional classpath files, after the selected project's entries
	 * @throws CoreException if no platform is imported or its dependencies cannot be obtained
	 */
	protected Collection<File> getCommerceClasspath(final ILaunchConfiguration configuration) throws CoreException {
		final IProject platform = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
		if (!platform.isOpen() || platform.getLocation() == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Import a Commerce platform before launching integration tests")); //$NON-NLS-1$
		}
		final Set<File> files = new LinkedHashSet<>();
		for (final String path : JavaRuntime.computeDefaultRuntimeClassPath(JavaCore.create(platform))) {
			files.add(new File(path));
		}
		final File home = platform.getLocation().toFile();
		try {
			final List<ExtensionHolder> extensions = new ArrayList<>(
					com.hybris.yps.hyeclipse.Activator.getDefault().getAllExtensionsForPlatform());
			extensions.sort(Comparator.comparing(ExtensionHolder::getName));
			for (final ExtensionHolder extension : extensions) {
				addExtensionClasspath(files, new File(extension.getPath()), extension.getName());
			}
			// The extension exporter omits core extensions, which live beneath platform/ext.
			for (final File extension : sortedChildren(new File(home, "ext"))) { //$NON-NLS-1$
				if (extension.isDirectory()) {
					addExtensionClasspath(files, extension, extension.getName());
				}
			}
			addJars(files, new File(home, "lib/dbdriver")); //$NON-NLS-1$
		} catch (RuntimeException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to compute Commerce integration test classpath", e)); //$NON-NLS-1$
		}
		return files;
	}

	/**
	 * Adds existing extension outputs, resources and libraries in runtime order.
	 * @param files ordered classpath entries to update
	 * @param directory extension directory
	 * @param name extension name
	 */
	protected void addExtensionClasspath(final Set<File> files, final File directory, final String name) {
		addIfExists(files, new File(directory, "eclipsebin/classes")); //$NON-NLS-1$
		addIfExists(files, new File(directory, "classes")); //$NON-NLS-1$
		addIfExists(files, new File(directory, "bin/" + name + "server.jar")); //$NON-NLS-1$ //$NON-NLS-2$
		addIfExists(files, new File(directory, "resources")); //$NON-NLS-1$
		addJars(files, new File(directory, "lib")); //$NON-NLS-1$
	}

	private void addIfExists(final Set<File> files, final File file) {
		if (file.exists()) {
			files.add(file);
		}
	}

	private void addJars(final Set<File> files, final File directory) {
		for (final File file : sortedChildren(directory)) {
			if (file.isFile() && file.getName().endsWith(".jar")) { //$NON-NLS-1$
				files.add(file);
			}
		}
	}

	private File[] sortedChildren(final File directory) {
		final File[] files = directory.listFiles();
		if (files == null) {
			return new File[0];
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		return files;
	}
}
