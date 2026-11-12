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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.jobs.UpdateWorkspaceJdkJob;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceConstants;

public class ProjectJavaRuntimeTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();
	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final List<IProject> projects = new ArrayList<>();
	private final List<IVMInstall> installations = new ArrayList<>();
	private IPreferenceStore store;
	private String originalSelection;
	private boolean selectionWasDefault;
	private IVMInstall originalDefault;
	private Map<String, String> originalOptions;
	private boolean autoBuild;
	private IVMInstall selected;

	@Before
	public void setUp() throws Exception {
		store = Activator.getDefault().getPreferenceStore();
		originalSelection = store.getString(HyEclipsePreferenceConstants.P_ANT_JDK);
		selectionWasDefault = store.isDefault(HyEclipsePreferenceConstants.P_ANT_JDK);
		originalDefault = JavaRuntime.getDefaultVMInstall();
		originalOptions = JavaCore.getOptions();
		autoBuild = ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		setAutoBuild(false);
		final IVMInstall workspace = installation("workspace");
		selected = installation("selected");
		JavaRuntime.setDefaultVMInstall(workspace, monitor, false);
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, JavaRuntime.getCompositeIdFromVM(selected));
	}

	@After
	public void tearDown() throws Exception {
		for (final IProject project : projects) {
			if (project.exists()) {
				project.delete(true, true, monitor);
			}
		}
		if (selectionWasDefault) {
			store.setToDefault(HyEclipsePreferenceConstants.P_ANT_JDK);
		} else {
			store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, originalSelection);
		}
		JavaRuntime.setDefaultVMInstall(originalDefault, monitor, false);
		for (final IVMInstall vm : installations) {
			vm.getVMInstallType().disposeVMInstall(vm.getId());
		}
		JavaCore.setOptions(new Hashtable<>(originalOptions));
		setAutoBuild(autoBuild);
	}

	@Test
	public void importRuntimeRepairMustUseTheAntPreferenceNotTheWorkspaceDefault() throws Exception {
		final IJavaProject project = project();
		project.setRawClasspath(new IClasspathEntry[0], monitor);
		final Method repair = Importer.class.getDeclaredMethod("fixMissingJavaRuntime",
				IProgressMonitor.class, IJavaProject.class);
		repair.setAccessible(true);
		repair.invoke(new Importer(), monitor, project);
		Assert.assertEquals(AntJavaRuntime.getJavaHome(), selected.getInstallLocation().getAbsoluteFile());
		Assert.assertEquals("Imported project must use the same registered JDK as Ant", selected,
				JavaRuntime.getVMInstall(project));
	}

	@Test
	public void compilerWarningAdjustmentMustNotChangeWorkspaceCompilerCompliance() throws Exception {
		final IJavaProject project = project();
		final Map<String, String> before = JavaCore.getOptions();
		final Method adjust = Importer.class.getDeclaredMethod("lowerJavaNotificationLevels", IJavaProject.class,
				double.class);
		adjust.setAccessible(true);
		adjust.invoke(new Importer(), project, 2211d);
		Assert.assertEquals("Import must not globally force Java 8", before, JavaCore.getOptions());
	}

	@Test
	public void newlyImportedProjectsUseTheCapturedJdkWithoutOptionalClasspathRepair() throws Exception {
		final IJavaProject javaProject = project();
		// Exercise the actual post-Smart-Import configuration seam, without invoking the optional repair phase.
		new Importer(null, selected).configureImportedProject(javaProject.getProject(), 2211d, monitor);
		Assert.assertEquals(selected, JavaRuntime.getVMInstall(javaProject));
		Assert.assertEquals(JavaCore.VERSION_21, javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true));
	}

	@Test
	public void appliesSelectedVmAndProjectComplianceWithoutChangingWorkspaceDefaults() throws Exception {
		final IJavaProject project = project();
		final IVMInstall workspaceDefault = JavaRuntime.getDefaultVMInstall();
		final Map<String, String> workspaceOptions = JavaCore.getOptions();
		ProjectJavaRuntime.apply(project, selected, monitor);
		Assert.assertEquals(selected, JavaRuntime.getVMInstall(project));
		Assert.assertEquals(JavaCore.VERSION_21, project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
		Assert.assertEquals(JavaCore.VERSION_21, project.getOption(JavaCore.COMPILER_SOURCE, true));
		Assert.assertEquals(JavaCore.VERSION_21, project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true));
		Assert.assertEquals(workspaceDefault, JavaRuntime.getDefaultVMInstall());
		Assert.assertEquals(workspaceOptions, JavaCore.getOptions());
	}

	@Test
	public void preservesSourceEntriesAndRuntimeMetadataAndRemovesDuplicateRuntimes() throws Exception {
		final IJavaProject project = project();
		final IClasspathAttribute[] attributes = new IClasspathAttribute[] {
				JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString()) };
		final IClasspathEntry source = JavaCore.newSourceEntry(project.getPath());
		final IClasspathEntry runtime = JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath(),
				new IAccessRule[0], attributes, true);
		project.setRawClasspath(new IClasspathEntry[] { source, runtime,
				JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(selected)) }, monitor);
		ProjectJavaRuntime.apply(project, selected, monitor);
		final IClasspathEntry[] after = project.getRawClasspath();
		Assert.assertEquals(2, after.length);
		Assert.assertEquals(source, after[0]);
		Assert.assertEquals(JavaRuntime.newJREContainerPath(selected), after[1].getPath());
		Assert.assertTrue(after[1].isExported());
		Assert.assertArrayEquals(attributes, after[1].getExtraAttributes());
	}

	@Test
	public void confirmedJobUpdatesClosedJavaProjectsAndLeavesNonJavaProjectsUntouched() throws Exception {
		final IJavaProject open = project();
		final IJavaProject closed = project();
		closed.getProject().close(monitor);
		final IProject plain = ResourcesPlugin.getWorkspace().getRoot().getProject("plain-jdk-test-" + UUID.randomUUID());
		projects.add(plain);
		plain.create(monitor);
		plain.open(monitor);
		final UpdateWorkspaceJdkJob job = new UpdateWorkspaceJdkJob(selected);
		job.schedule();
		job.join();
		Assert.assertTrue(job.getResult().toString(), job.getResult().isOK());
		Assert.assertEquals(selected, JavaRuntime.getVMInstall(open));
		Assert.assertFalse(closed.getProject().isOpen());
		Assert.assertFalse(plain.hasNature(JavaCore.NATURE_ID));
		closed.getProject().open(monitor);
		Assert.assertEquals(selected, JavaRuntime.getVMInstall(closed));
		Assert.assertEquals(JavaCore.VERSION_21, closed.getOption(JavaCore.COMPILER_COMPLIANCE, true));
	}

	@Test
	public void pinnedAntAndImporterSelectionsSurvivePreferenceChanges() throws Exception {
		final ShellImporter ant = new ShellImporter(selected);
		final Importer importer = new Importer(null, selected);
		final IVMInstall changed = installation("changed");
		final File otherHome = temporary.newFolder("other-jdk");
		Files.createDirectories(otherHome.toPath().resolve("bin"));
		Files.createFile(otherHome.toPath().resolve("bin/javac"));
		changed.setInstallLocation(otherHome);
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, JavaRuntime.getCompositeIdFromVM(changed));
		Assert.assertEquals(selected.getInstallLocation().getAbsolutePath(),
				ant.antProcess(temporary.getRoot()).environment().get(AntJavaRuntime.JAVA_HOME));
		final IJavaProject project = project();
		project.setRawClasspath(new IClasspathEntry[0], monitor);
		final Method repair = Importer.class.getDeclaredMethod("fixMissingJavaRuntime",
				IProgressMonitor.class, IJavaProject.class);
		repair.setAccessible(true);
		repair.invoke(importer, monitor, project);
		Assert.assertEquals(selected, JavaRuntime.getVMInstall(project));
	}

	@Test
	public void canceledWorkspaceUpdatesDoNotChangeProjects() throws Exception {
		final IJavaProject project = project();
		final IClasspathEntry[] before = project.getRawClasspath();
		final NullProgressMonitor canceled = new NullProgressMonitor();
		canceled.setCanceled(true);
		Assert.assertThrows(org.eclipse.core.runtime.OperationCanceledException.class,
				() -> ProjectJavaRuntime.updateWorkspace(selected, canceled));
		Assert.assertArrayEquals(before, project.getRawClasspath());
	}

	@Test
	public void importRepairPreservesExistingExplicitRuntimeWithoutAWorkspaceConfirmation() throws Exception {
		final IJavaProject project = project();
		final IVMInstall before = JavaRuntime.getVMInstall(project);
		final Method repair = Importer.class.getDeclaredMethod("fixMissingJavaRuntime",
				IProgressMonitor.class, IJavaProject.class);
		repair.setAccessible(true);
		repair.invoke(new Importer(), monitor, project);
		Assert.assertEquals(before, JavaRuntime.getVMInstall(project));
	}

	private IVMInstall installation(final String name) {
		final IVMInstallType type = JavaRuntime.getVMInstallTypes()[0];
		final VMStandin definition = new VMStandin(type, UUID.randomUUID().toString());
		definition.setName(name + "-" + definition.getId());
		definition.setInstallLocation(new File(System.getProperty("java.home")));
		final IVMInstall vm = definition.convertToRealVM();
		installations.add(vm);
		return vm;
	}

	private IJavaProject project() throws Exception {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("jdk-test-" + UUID.randomUUID());
		projects.add(project);
		project.create(monitor);
		project.open(monitor);
		final IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, monitor);
		final IJavaProject javaProject = JavaCore.create(project);
		javaProject.setRawClasspath(new IClasspathEntry[] {
				JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath()) }, monitor);
		return javaProject;
	}

	private void setAutoBuild(final boolean enabled) throws Exception {
		final IWorkspaceDescription settings = ResourcesPlugin.getWorkspace().getDescription();
		settings.setAutoBuilding(enabled);
		ResourcesPlugin.getWorkspace().setDescription(settings);
	}
}
