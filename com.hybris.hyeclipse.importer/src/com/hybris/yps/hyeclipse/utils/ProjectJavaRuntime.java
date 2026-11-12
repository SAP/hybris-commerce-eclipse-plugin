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
package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.wizards.Messages;

/** Applies the shared JDK selection without changing workspace defaults or unrelated classpath entries. */
public final class ProjectJavaRuntime {
	private static final Pattern VERSION = Pattern.compile("^(1\\.[0-9]+|[0-9]+)");
	private static final String RELEASE_FILE = "release"; //$NON-NLS-1$
	private static final String JAVA_VERSION = "JAVA_VERSION"; //$NON-NLS-1$
	private static final long LOCK_WAIT_MILLIS = 100;
	private static final ReentrantLock CHANGE_LOCK = new ReentrantLock(true);

	private ProjectJavaRuntime() {
		/* intentionally empty */
	}

	/**
	 * Serializes import and confirmed workspace JDK changes without holding a workspace rule during Ant/child Jobs.
	 * @param monitor cancellation monitor
	 * @return scope to close after the entire operation
	 * @throws InterruptedException if interrupted while waiting
	 */
	public static ChangeScope beginChange(final IProgressMonitor monitor) throws InterruptedException {
		checkCanceled(monitor);
		while (!CHANGE_LOCK.tryLock(LOCK_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
			checkCanceled(monitor);
		}
		if (monitor.isCanceled()) {
			CHANGE_LOCK.unlock();
			throw new OperationCanceledException();
		}
		return new ChangeScope();
	}

	/** Same-thread scope for the import/JDK-change lock; nested importer calls are supported. */
	public static final class ChangeScope implements AutoCloseable {
		private boolean closed;

		private ChangeScope() {
			/* intentionally empty */
		}

		@Override
		public void close() {
			if (!closed) {
				closed = true;
				CHANGE_LOCK.unlock();
			}
		}
	}

	/**
	 * @param vm selected registered JDK
	 * @return compiler level supported by both the JDK and Eclipse
	 * @throws IOException if the JDK/version is unavailable or unsupported
	 */
	public static String compilerCompliance(final IVMInstall vm) throws IOException {
		final File home = AntJavaRuntime.getJavaHome(vm);
		String version = vm instanceof IVMInstall2 ? ((IVMInstall2) vm).getJavaVersion() : null;
		if (StringUtils.isBlank(version)) {
			// Some VM types do not expose a version, but JDK installations provide it in their release metadata.
			final Properties release = new Properties();
			try (final InputStream input = Files.newInputStream(new File(home, RELEASE_FILE).toPath())) {
				release.load(input);
			}
			version = StringUtils.strip(release.getProperty(JAVA_VERSION), "\""); //$NON-NLS-1$
		}
		final Matcher matcher = VERSION.matcher(StringUtils.defaultString(version));
		if (!matcher.find()) {
			throw new IOException("Cannot determine compiler compliance for JDK " + vm.getName());
		}
		String compliance = matcher.group(1);
		if ("8".equals(compliance)) { //$NON-NLS-1$
			compliance = JavaCore.VERSION_1_8;
		}
		if (!JavaCore.isSupportedJavaVersion(compliance)) {
			throw new IOException("Eclipse does not support the selected JDK's compiler level: " + compliance);
		}
		return compliance;
	}

	/**
	 * Replaces all JRE references with one selected-VM container, preserving the first entry's metadata and position.
	 * @param project open Java project
	 * @param vm captured JDK installation
	 * @param monitor cancellation monitor
	 * @throws JavaModelException if the JDK or project configuration is invalid
	 */
	public static void apply(final IJavaProject project, final IVMInstall vm, final IProgressMonitor monitor)
			throws JavaModelException {
		final String compliance;
		try {
			compliance = compilerCompliance(vm);
		} catch (IOException e) {
			throw new JavaModelException(new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Could not configure the selected project JDK", e)));
		}
		checkCanceled(monitor);
		final IClasspathEntry[] before = project.getRawClasspath();
		final List<IClasspathEntry> after = new ArrayList<>();
		boolean replaced = false;
		for (final IClasspathEntry entry : before) {
			final String first = entry.getPath().segment(0);
			final boolean runtime = (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
					&& JavaRuntime.JRE_CONTAINER.equals(first))
					|| (entry.getEntryKind() == IClasspathEntry.CPE_VARIABLE && JavaRuntime.JRELIB_VARIABLE.equals(first));
			if (!runtime) {
				after.add(entry);
			} else if (!replaced) {
				after.add(JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(vm), entry.getAccessRules(),
						entry.getExtraAttributes(), entry.isExported()));
				replaced = true;
			}
		}
		if (!replaced) {
			after.add(JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(vm)));
		}
		final IClasspathEntry[] entries = after.toArray(new IClasspathEntry[0]);
		if (!Arrays.equals(before, entries)) {
			project.setRawClasspath(entries, monitor);
		}
		final Map<String, String> options = new HashMap<>(project.getOptions(false));
		JavaCore.setComplianceOptions(compliance, options);
		if (JavaCore.VERSION_1_8.equals(compliance)) {
			options.put(JavaCore.COMPILER_RELEASE, JavaCore.DISABLED);
		}
		project.setOptions(options);
	}

	/**
	 * @param vm JDK confirmed by the user
	 * @param monitor cancellation monitor
	 * @return aggregated result; non-Java projects are untouched and closed Java projects are reclosed
	 * @throws InterruptedException if interrupted while waiting for an import
	 */
	public static IStatus updateWorkspace(final IVMInstall vm, final IProgressMonitor monitor)
			throws InterruptedException {
		try (final ChangeScope scope = beginChange(monitor)) {
			try {
				compilerCompliance(vm);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ProjectJavaRuntime_update_failed, e);
			}
			final MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK,
					Messages.ProjectJavaRuntime_update_failed, null);
			Job.getJobManager().beginRule(ResourcesPlugin.getWorkspace().getRoot(), monitor);
			try {
				final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				final SubMonitor progress = SubMonitor.convert(monitor, projects.length);
				for (final IProject project : projects) {
					checkCanceled(progress);
					boolean opened = false;
					try {
						if (!description(project, progress).hasNature(JavaCore.NATURE_ID)) {
							progress.worked(1);
							continue;
						}
						if (!project.isOpen()) {
							project.open(progress.split(0));
							opened = true;
						}
						apply(JavaCore.create(project), vm, progress.split(1));
					} catch (CoreException e) {
						result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								"Could not update JDK for project " + project.getName(), e));
					} finally {
						if (opened) {
							try {
								project.close(null);
							} catch (CoreException e) {
								result.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
										"Could not reclose project " + project.getName(), e));
							}
						}
					}
				}
			} finally {
				Job.getJobManager().endRule(ResourcesPlugin.getWorkspace().getRoot());
				monitor.done();
			}
			return result.isOK() ? Status.OK_STATUS : result;
		}
	}

	private static IProjectDescription description(final IProject project, final IProgressMonitor monitor)
			throws CoreException {
		if (project.isOpen()) {
			return project.getDescription();
		}
		// Closed projects reject getDescription(); inspect the saved descriptor without opening non-Java projects.
		try (final InputStream input = EFS.getStore(project.getLocationURI())
				.getChild(IProjectDescription.DESCRIPTION_FILE_NAME).openInputStream(EFS.NONE, monitor)) {
			return ResourcesPlugin.getWorkspace().loadProjectDescription(input);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Could not read closed project description: " + project.getName(), e));
		}
	}

	private static void checkCanceled(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
