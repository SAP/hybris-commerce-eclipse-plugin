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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.progress.IProgressConstants;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;

public class Importer {

	private static final String WARNING_MSG = "warning";
	private static final String CONFIG_FOLDER = "config";
	private static Activator plugin = Activator.getDefault();
	private static final boolean DEBUG = Activator.getDefault().isDebugging();

	private static final String HYBRIS_NATURE_ID = "com.hybris.hyeclipse.tsv.hybris";
	private static final String SPRING_NATURE_ID = "org.springframework.ide.eclipse.core.springnature";
	private static final String BROKEN_WST_SETTINGS_FILE = ".settings/org.eclipse.wst.validation.prefs";
	private static final String BROKEN_WST_SETTINGS_FILE_EXT = "promotionengineservices";
	private static final String SPRINGBEANS_FILE = ".springBeans";

	private static final double JVM8_VERSION = 5.6d;
	private static final double JVM11_VERSION = 1811d;
	Pattern platformVersionPattern = Pattern.compile("[0-9]*\\.[0-9]*");

	public void resetProjectsFromLocalExtensions(final File platformHome, final IProgressMonitor monitor,
			final boolean fixClasspath, final boolean removeHybrisGenerator, final boolean createWorkingSets,
			final boolean useMultiThread, final boolean skipJarScanning) throws CoreException, InterruptedException {
		com.hybris.hyeclipse.commons.Activator.disableProjectNatureSolutionLookup();
		plugin.resetPlatform(platformHome.getAbsolutePath());

		importExtensionsNotInWorkspace(monitor, platformHome);
		closeProjectsThatAreNotReferenced(monitor, platformHome);

		if (fixClasspath) {
			fixMissingProjectDependencies(monitor);
			fixMissingProjectResources(monitor, platformHome);
			fixProjectClasspaths(monitor);
		}

		if (removeHybrisGenerator) {
			fixBuilders(monitor);
		}

		if (createWorkingSets) {
			WorkingSetsUtils.organizeWorkingSetsFromExtensionDirectories(monitor);
		}

		if (useMultiThread) {
			UseMultiThreadUtils.useMultiThread(platformHome);
		}

		if (skipJarScanning) {
			SkipJarScanningUtils.skipJarScanning(platformHome);
		}

		fixSpringBeans(monitor);
		com.hybris.hyeclipse.commons.Activator.restoreProjectNatureSolutionLookup();
	}

	private void importExtensionsNotInWorkspace(final IProgressMonitor monitor, final File platformHome)
			throws CoreException, InterruptedException {
		Activator.log("Retrieving extensions not in workspace");

		final double version = getPlatformVersion(platformHome);

		final Collection<ExtensionHolder> extensions = FixProjectsUtils
				.getExtensionsNotInWorkspace(platformHome.getAbsolutePath());
		if (!extensions.isEmpty()) {
			monitor.setTaskName("Importing extensions");
			monitor.beginTask("Importing extensions", extensions.size());
			int progress = 0;
			for (final ExtensionHolder extensionHolder : extensions) {
				final IPath extP = Path.fromOSString(extensionHolder.getPath());
				final IPath projectFilepath = extP.append("/" + Constants.DOT_PROJECT);
				final boolean projectFileExist = projectFilepath.toFile().exists();
				if (projectFileExist) {
					Activator.log("Importing Eclipse project [" + extensionHolder + "]");
					importProject(monitor, version, extP);
					// fix the modules (e.g. remove hmc module if not needed)
				} else if (isHybrisExtension(extP)) {
					Activator.log(MessageFormat.format("Trying to create project [{0}] in IDE", extensionHolder));
					importProject(monitor, version, extP);

				}
				fixModules(monitor, extensionHolder);
				progress++;
				monitor.worked(progress);
			}
		}
	}

	/**
	 * Method checks if given folder is SAP Commerce extension. by checking if there
	 * is {@code extensioninfo.xml} or {@code localextensions.xml}. Latter one
	 * defines
	 *
	 * <pre>
	 * config
	 * </pre>
	 *
	 * folder, which may be renamed by user.
	 *
	 * @param path to folder found by import process as an extension
	 * @return <code>true</code> if folder contains
	 *         {@code Importer#HYBRIS_EXTENSION_FILE}
	 */
	protected boolean isHybrisExtension(final IPath path) {
		return path.append(Constants.EXTENSION_INFO_XML).toFile().exists()
				|| path.append(Constants.LOCAL_EXTENSIONS_XML).toFile().exists();
	}

	private void closeProjectsThatAreNotReferenced(final IProgressMonitor monitor, final File platformHome) {
		if (DEBUG) {
			Activator.log("Retrieving projects not in localextensions using platformhome ["
					+ platformHome.getAbsolutePath() + "]");
		}

		final Set<IProject> projectsToClose = FixProjectsUtils.getProjectsNotInLocalExtensionsFile();

		final Set<String> referencingProjects = projectsToClose.stream()
				.flatMap(p -> Arrays.stream(p.getReferencingProjects())).map(IProject::getName)
				.collect(Collectors.toSet());

		// close projects from the above set that are not referenced by a
		// project that is not scheduled for closing
		projectsToClose.stream().forEach(p -> {

			if (!referencingProjects.contains(p.getName())) {
				try {
					p.close(monitor);
				} catch (final CoreException e) {
					Activator.logError(MessageFormat.format("could not close project [{0}]", p.getName()), e);
				}
			}
		});
	}

	@SuppressWarnings("restriction")
	private IProject importProject(final IProgressMonitor monitor, final double version, final IPath extensionFolder)
			throws CoreException, InterruptedException {
		final SubMonitor progress = SubMonitor.convert(monitor, 12);
		final SmartImportJob importJob = new SmartImportJob(extensionFolder.toFile(), Collections.emptySet(), true,
				false);
		importJob.setProperty(IProgressConstants.PROPERTY_IN_DIALOG, true);
		importJob.schedule();
		importJob.join(10_000L, monitor);
		final String name = extensionFolder.lastSegment();

		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.exists()) {
			try {
				project.create(null, progress.newChild(3));				
			} catch (ResourceException e) {
				if (e.getStatus().getCode() == IResourceStatus.RESOURCE_EXISTS) {
					project.open(monitor);
				} else {
					Activator.logError(MessageFormat.format("could not open project [{0}]", project.getName()), e);
				}
			}
		}
		final IJavaProject javaProject = JavaCore.create(project);
		javaProject.save(monitor, DEBUG);
		if (JavaProject.hasJavaNature(project)) {
			javaProject.open(monitor);			
		}
		fixProjectCompilerSettings(project, javaProject, version);
		project.open(progress.newChild(3));
		FixProjectsUtils.removeBuildersFromProject(progress.newChild(3), project);
		addHybrisNature(project, progress.newChild(3));
		return project;
	}

	protected double getPlatformVersion(final File platformHome) {
		final java.nio.file.Path buildNumberPath = platformHome.toPath().resolve("build.number");
		Double platformVersion = 6.3d;
		String propertyVersion = "";
		try (FileReader fr = new FileReader(buildNumberPath.toFile())) {
			final Properties platformProps = new Properties();
			platformProps.load(fr);
			propertyVersion = platformProps.getProperty("version", platformVersion.toString());
			platformVersion = convertPlatformVersion(propertyVersion, platformVersion);
		} catch (final IOException e) {
			throw new IllegalStateException(MessageFormat.format("Error reading file {0}", buildNumberPath), e);
		}
		return platformVersion;
	}

	protected Double convertPlatformVersion(final String platformVersion, final Double def) {
		final Matcher m = platformVersionPattern.matcher(platformVersion);
		Double ret = def;
		if (m.find()) {
			try {
				ret = Double.valueOf(m.group());
			} catch (final NumberFormatException e) {
				Activator.log(MessageFormat.format(
						"Platform version not in SAP format [dd].[dd] but {0}. Falling back to version {1}", m.group(),
						platformVersion));
			}
		}
		return ret;
	}

	private void fixProjectCompilerSettings(final IProject project, final IJavaProject javaProject,
			final double platformVersion) {
		// don't fix project or custom extensions
		if (FixProjectsUtils.isAPlatformExtension(project) && !FixProjectsUtils.isATemplateExtension(project)) {
			lowerJavaNotificationLevels(javaProject, platformVersion);

		}
	}

	private void lowerJavaNotificationLevels(final IJavaProject javaProject, final double platformVersion) {
		javaProject.setOption(JavaCore.COMPILER_PB_EMPTY_STATEMENT, WARNING_MSG);
		javaProject.setOption(JavaCore.COMPILER_PB_AUTOBOXING, WARNING_MSG);
		javaProject.setOption(JavaCore.COMPILER_PB_UNUSED_LOCAL, WARNING_MSG);
		javaProject.setOption(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, WARNING_MSG);
		javaProject.setOption(JavaCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK, WARNING_MSG);

		// make sure all 1.7 settins are substitute for 1.8
		// for versions 5.6 and higher
		final Hashtable<String, String> javaCompilerOptions = JavaCore.getOptions();
		if (platformVersion >= JVM8_VERSION) {
			JavaCore.setComplianceOptions("1.8", javaCompilerOptions);
		} else if (platformVersion >= JVM11_VERSION) {
			JavaCore.setComplianceOptions("11", javaCompilerOptions);
		}
		if (!javaCompilerOptions.isEmpty()) {
			JavaCore.setOptions(javaCompilerOptions);
		}
	}

	private void addHybrisNature(final IProject project, final IProgressMonitor monitor) throws CoreException {
		final IProjectDescription description = project.getDescription();

		final Set<String> natSet = new HashSet<>(Arrays.asList(description.getNatureIds()));
		natSet.add(HYBRIS_NATURE_ID);
		natSet.add(JavaCore.NATURE_ID);
		final String[] newNatures = natSet.toArray(new String[natSet.size()]);
		description.setNatureIds(newNatures);
		project.setDescription(description, monitor);
	}

	private void fixBuilders(final IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName("Fixing builders");
		final Set<IProject> hybrisProjects = FixProjectsUtils.getAllOpenHybrisProjects();
		monitor.beginTask("Fixing builders", hybrisProjects.size());
		int i = 0;
		for (final IProject project : hybrisProjects) {
			FixProjectsUtils.removeBuildersFromProject(monitor, project);
			monitor.internalWorked(i++);
		}
	}

	/**
	 * We remove any libraries from the Project that don't exist and automatically
	 * add any libraries from the lib directory of the extension
	 *
	 * @param monitor
	 */
	private void fixProjectClasspaths(final IProgressMonitor monitor) {
		if (ResourcesPlugin.getWorkspace().getRoot().getProjects() == null) {
			return;
		}

		monitor.setTaskName("Fixing project classpaths");
		monitor.beginTask("Fixing project classpaths", ResourcesPlugin.getWorkspace().getRoot().getProjects().length);
		monitor.worked(0);
		int projCnt = 0;
		for (final IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
				if (FixProjectsUtils.isAHybrisExtension(project) && project.isOpen()
						&& project.hasNature(JavaCore.NATURE_ID)) {
					final IJavaProject javaProject = JavaCore.create(project);
					removeJarFilesThatDontExist(monitor, project, javaProject);
					addJarFilesNotInClasspath(monitor, project, javaProject);
					FixProjectsUtils.addSourceDirectoriesIfExisting(monitor, project, javaProject);
					FixProjectsUtils.removeSourceDirectoriesIfNotExisting(monitor, project, javaProject);
					FixProjectsUtils.setOutputDirectory(monitor, project, javaProject);
					fixBackofficeJars(monitor, javaProject);
					fixAddons(monitor, javaProject);
					fixMissingJavaRuntime(monitor, javaProject);
				}
			} catch (final CoreException e) {
				throw new IllegalStateException(e);
			}
			projCnt++;
			monitor.worked(projCnt);
		}
	}

	private boolean isSpringProject(final IProject project) {
		boolean springProject = false;
		try {
			springProject = project.isOpen() && project.hasNature(SPRING_NATURE_ID);
		} catch (final CoreException e) {
			Activator.log(MessageFormat.format("could not check project {0} if that is spring one", project.getName()));
		}
		return springProject;
	}

	private void fixSpringBeans(final IProgressMonitor monitor) {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot root = workspace.getRoot();
		final IProject[] projects = root.getProjects();
		if (projects == null) {
			return;
		}

		monitor.setTaskName("Fixing Spring Beans");
		monitor.beginTask("Fixing Spring Beans", projects.length);
		monitor.worked(0);
		for (final IProject project : projects) {
			monitor.worked(1);

			if (!FixProjectsUtils.isAHybrisExtension(project) || isSpringProject(project)) {
				continue;
			}

			final File location = project.getLocation().toFile();

			final File springBeansFile = new File(location, SPRINGBEANS_FILE);
			// rewriting the lines between : <configs> & </configs>
			// It is very crude, but it works since the file should be
			// formatted nicely.
			final File springBeansFileNew = new File(location, SPRINGBEANS_FILE + ".new");

			// We don't care about the existing one. Just overwrite it
			// bluntly
			try (FileWriter fw = new FileWriter(springBeansFileNew);) {
				fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				fw.write("<beansProjectDescription>\n");
				fw.write("\t<version>1</version>\n");
				fw.write("\t<pluginVersion><![CDATA[3.1.0.201210040510-RELEASE]]></pluginVersion>\n");
				fw.write("\t<configSuffixes>\n");
				fw.write("\t\t<configSuffix><![CDATA[xml]]></configSuffix>\n");
				fw.write("\t</configSuffixes>\n");
				fw.write("\t<enableImports><![CDATA[true]]></enableImports>\n");

				// Recursively find *-spring.xml files
				final String[] beansFiles = getAllSpringXmlFiles(project);

				fw.write("\t<configs>\n");
				for (final String beansFile : beansFiles) {
					final File beanFile = new File(location, beansFile);
					if (!beanFile.exists()) {
						continue;
					}

					fw.write("\t\t<config>");
					fw.write(beansFile);
					fw.write("</config>");
					fw.write("\n");
				}
				fw.write("\t</configs>\n");

				fw.write("\t<configSets>\n");
				fw.write("\t</configSets>\n");
				fw.write("</beansProjectDescription>\n");

			} catch (final IOException e) {
				throw new IllegalStateException(
						"Error while fixing the compiler settings in " + springBeansFile.toString(), e);
			}

			if (!springBeansFileNew.renameTo(springBeansFile)) {
				Activator.log(
						MessageFormat.format("Could not move file {0} -> {1}", springBeansFileNew, springBeansFile));
			}

		}

	}

	private String[] getAllSpringXmlFiles(final IProject project) {
		final String projectName = project.getName();
		return new String[] { //
				"resources/" + projectName + "-spring.xml", //
				"web/webroot/WEB-INF/" + projectName + "-web-spring.xml", //
		};
	}

	/**
	 * Sometimes the project configuration is corrupt and a Java runtime is not on
	 * the classpath
	 *
	 * @param monitor
	 * @param javaProject
	 * @throws JavaModelException
	 */
	private void fixMissingJavaRuntime(final IProgressMonitor monitor, final IJavaProject javaProject)
			throws JavaModelException {

		if (!CONFIG_FOLDER.equals(javaProject.getProject().getName())) {
			final IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
			boolean found = false;
			for (final IClasspathEntry classpathEntry : classPathEntries) {
				// fix missing runtime
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
						&& classpathEntry.getPath().toString().startsWith("org.eclipse.jdt.launching.JRE_CONTAINER")) {
					found = true;
					break;
				}
			}

			if (!found) {
				final IClasspathEntry entry = JavaCore
						.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"), false);
				final Set<IClasspathEntry> entries = new HashSet<>(Arrays.asList(classPathEntries));
				entries.add(entry);
				FixProjectsUtils.setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject,
						monitor);
			}
		}
	}

	/**
	 * Some addons don't have their classpath correctly configured in Eclipse and
	 * won't compile
	 *
	 * @param monitor
	 * @param javaProject
	 * @throws JavaModelException
	 */
	private void fixAddons(final IProgressMonitor monitor, final IJavaProject javaProject) throws JavaModelException {

		final String projectName = javaProject.getProject().getName();

		if ("orderselfserviceaddon".equals(projectName) || "notificationaddon".equals(projectName)
				|| "customerinterestsaddon".equals(projectName) || "consignmenttrackingaddon".equals(projectName)) {

			final IProject acceleratorstorefrontcommonsProject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject("acceleratorstorefrontcommons");
			if (acceleratorstorefrontcommonsProject != null && acceleratorstorefrontcommonsProject.exists()
					&& !javaProject.isOnClasspath(acceleratorstorefrontcommonsProject)) {
				FixProjectsUtils.addToClassPath(acceleratorstorefrontcommonsProject, IClasspathEntry.CPE_PROJECT,
						javaProject, monitor);
			}
		}

		if ("stocknotificationaddon".equals(projectName)) {
			final IProject notificationfacadesProject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject("notificationfacades");
			if (notificationfacadesProject != null && notificationfacadesProject.exists()
					&& !javaProject.isOnClasspath(notificationfacadesProject)) {
				FixProjectsUtils.addToClassPath(notificationfacadesProject, IClasspathEntry.CPE_PROJECT, javaProject,
						monitor);
			}
		}
	}

	/**
	 * Make sure all the relevant backoffice jars are exported
	 *
	 * @param monitor
	 * @param javaProject
	 * @throws JavaModelException
	 */
	private void fixBackofficeJars(final IProgressMonitor monitor, final IJavaProject javaProject)
			throws JavaModelException {
		if ("backoffice".equalsIgnoreCase(javaProject.getProject().getName())) {
			final List<IClasspathEntry> entries = new LinkedList<>();
			final IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
			boolean change = false;
			for (final IClasspathEntry classpathEntry : classPathEntries) {
				// fix jar files
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (classpathEntry.getPath().toString()
						.contains("/backoffice/web/webroot/WEB-INF/lib/backoffice-core-")
						|| classpathEntry.getPath().toString()
								.contains("/backoffice/web/webroot/WEB-INF/lib/backoffice-widgets-")
						|| classpathEntry.getPath().toString()
								.contains("/backoffice/web/webroot/WEB-INF/lib/cockpitframework-")
						|| classpathEntry.getPath().toString()
								.contains("/backoffice/web/webroot/WEB-INF/lib/cockpitcore-")
						|| classpathEntry.getPath().toString()
								.contains("/backoffice/web/webroot/WEB-INF/lib/cockpittesting-")
						|| classpathEntry.getPath().toString()
								.contains("/backoffice/web/webroot/WEB-INF/lib/cockpitwidgets-")
						|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/cockpit-")
						|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/zk")
						|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/zul-")
						|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/zcommon-"))
						&& !classpathEntry.isExported()) {
					change = true;
					final IClasspathEntry clonedEntry = JavaCore.newLibraryEntry(classpathEntry.getPath(),
							classpathEntry.getSourceAttachmentPath(), classpathEntry.getSourceAttachmentRootPath(),
							classpathEntry.getAccessRules(), classpathEntry.getExtraAttributes(), true);
					entries.add(clonedEntry);
					continue;
				}
				entries.add(classpathEntry);
			}
			if (change) {
				FixProjectsUtils.setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject,
						monitor);
			}
		}
	}

	private void removeJarFilesThatDontExist(final IProgressMonitor monitor, final IProject project,
			final IJavaProject javaProject) throws JavaModelException {
		final IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		final List<IClasspathEntry> newClassPathEntries = new LinkedList<>();
		boolean changedClassPath = false;
		for (final IClasspathEntry classpathEntry : classPathEntries) {
			// fix jar files
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				final File classpathEntryFile = classpathEntry.getPath().toFile();

				// remove JAR if it doesn't exist, only do this if the jar file is located in
				// this project, we leave jars references from different projects
				if (classpathEntryFile.getPath().endsWith(".jar")
						&& classpathEntryFile.getPath().startsWith("/" + project.getName() + "/")
						&& !project.getFile(classpathEntryFile.getPath().replace("/" + project.getName() + "/", "/"))
								.exists()) {
					changedClassPath = true;
					if (DEBUG) {
						Activator.log("libary [" + classpathEntry.getPath() + "] not found for project [ "
								+ project.getName() + "]");
					}
				} else {
					newClassPathEntries.add(classpathEntry);
				}
			} else {
				newClassPathEntries.add(classpathEntry);
			}
		}

		// we have a change to the classpath so now push the change
		if (changedClassPath) {
			if (newClassPathEntries.isEmpty()) {
				FixProjectsUtils.setClasspath(new IClasspathEntry[0], javaProject, monitor);
			} else {
				FixProjectsUtils.setClasspath(
						newClassPathEntries.toArray(new IClasspathEntry[newClassPathEntries.size()]), javaProject,
						monitor);
			}
		}
	}

	private void fixMissingProjectDependencies(final IProgressMonitor monitor) throws JavaModelException {
		final Set<ExtensionHolder> extensions = FixProjectsUtils.getAllExtensionsForPlatform();
		final Set<IProject> projects = FixProjectsUtils.getAllOpenHybrisProjects();
		for (final IProject project : projects) {
			for (final ExtensionHolder extHolder : extensions) {
				if (extHolder.getName().equalsIgnoreCase(project.getName())) {
					addMissingProjectDependencies(monitor, project, extHolder);
				}
			}
		}
	}

	private void addMissingProjectDependencies(final IProgressMonitor monitor, final IProject project,
			final ExtensionHolder extHolder) throws JavaModelException {
		final IJavaProject javaProject = JavaCore.create(project);

		final IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		if (!extHolder.getDependentExtensions().isEmpty()) {
			for (final String ext : extHolder.getDependentExtensions()) {
				boolean found = false;
				for (final IClasspathEntry classpathEntry : classPathEntries) {
					// fix jar files
					if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT
							&& classpathEntry.getPath().toString().replaceFirst("/", "").equalsIgnoreCase(ext)) {
						found = true;
						break;
					}
				}
				if (!found) {
					final IProject dependentProject = ResourcesPlugin.getWorkspace().getRoot().getProject(ext);
					if (dependentProject.exists() && dependentProject.isOpen()) {
						FixProjectsUtils.addToClassPath(dependentProject, IClasspathEntry.CPE_PROJECT, javaProject,
								monitor);
					}
				}
			}
		}
	}

	private void fixMissingProjectResources(final IProgressMonitor monitor, final File platformHome)
			throws CoreException {
		final Set<IProject> projects = FixProjectsUtils.getAllHybrisProjects();
		final IProject config = projects.stream().filter(p -> CONFIG_FOLDER.equals(p.getName())).findFirst()
				.orElse(null);
		final IProject platform = projects.stream().filter(p -> Constants.PLATFROM.equals(p.getName())).findFirst()
				.orElse(null);

		if (config != null && platform != null) {
			final IFile activeRoleEnvPropertyFile = platform.getFile("active-role-env.properties");
			final IFile instancePropertiesLink = config.getFile("instance.properties");

			if (instancePropertiesLink.exists()) {
				// Remove the instance.properties link if existing
				instancePropertiesLink.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, monitor);
			}
			if (activeRoleEnvPropertyFile.exists()) {
				// extract currently enabled role & instance from active-role-env.properties
				// https://wiki.hybris.com/display/RD/hybris+server+roles)
				final Properties prop = new Properties();
				try (InputStream input = new FileInputStream(activeRoleEnvPropertyFile.getLocation().toFile())) {
					prop.load(input);
				} catch (final IOException e) {
					throw new IllegalStateException(e);
				}
				final String activeRole = prop.getProperty("ACTIVE_ROLE").replace("${platformhome}",
						platformHome.toString());
				final String activeInstance = prop.getProperty("ACTIVE_ROLE_INSTANCE").replace("${platformhome}",
						platformHome.toString());

				// Create the instance.properties link
				final File hybrisRootDir = platform.getLocation().toFile().getParentFile().getParentFile();
				final File instanceConfigDir = new File(
						new File(new File(new File(hybrisRootDir, "roles"), activeRole), activeInstance),
						CONFIG_FOLDER);
				final File instancePropertiesFile = new File(instanceConfigDir, "instance.properties");

				final IPath location = new Path(instancePropertiesFile.toString());
				if (DEBUG) {
					Activator.log("location = " + location.toString());
					Activator.log("instancePropertiesLink = " + instancePropertiesLink.toString());
				}

				instancePropertiesLink.createLink(location, IResource.NONE, null);
			}
		}
	}

	private void addJarFilesNotInClasspath(final IProgressMonitor monitor, final IProject project,
			final IJavaProject javaProject) throws CoreException {
		addMembersOfFolderToClasspath("/lib", monitor, javaProject);
		addMembersOfFolderToClasspath("/web/webroot/WEB-INF/lib", monitor, javaProject);
		// check if this is a backoffice extension
		final IFolder backofficeFolder = javaProject.getProject().getFolder("/resources/backoffice");
		if (backofficeFolder != null && backofficeFolder.exists()) {
			final IResource backofficeJar = backofficeFolder
					.findMember(javaProject.getProject().getName() + "_bof.jar");
			if (backofficeJar != null && backofficeJar.exists()
					&& !isClasspathEntryForJar(javaProject, backofficeJar)) {
				Activator.log("Adding library [" + backofficeJar.getFullPath() + "] to classpath for project ["
						+ javaProject.getProject().getName() + "]");
				FixProjectsUtils.addToClassPath(backofficeJar, IClasspathEntry.CPE_LIBRARY, javaProject, monitor);
			}
		}

		// add db drivers for platform/lib/dbdriver directory
		if (Constants.PLATFROM.equalsIgnoreCase(project.getName())) {
			addMembersOfFolderToClasspath("/lib/dbdriver", monitor, javaProject);
		}
	}

	private void addMembersOfFolderToClasspath(final String path, final IProgressMonitor monitor,
			final IJavaProject javaProject) throws CoreException {
		final IFolder folder = javaProject.getProject().getFolder(path);
		if (folder != null && folder.exists()) {
			for (final IResource res : folder.members()) {
				// check if this Resource is on the classpath
				if (res.getFileExtension() != null && "jar".equals(res.getFileExtension()) && res.exists()
						&& !javaProject.isOnClasspath(res)) {
					FixProjectsUtils.addToClassPath(res, IClasspathEntry.CPE_LIBRARY, javaProject, monitor);
				}
			}
		}
	}

	/**
	 *
	 * @param javaProject
	 * @param jar
	 * @return
	 * @throws JavaModelException
	 */
	private boolean isClasspathEntryForJar(final IJavaProject javaProject, final IResource jar)
			throws JavaModelException {
		final IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		if (classPathEntries != null) {
			for (final IClasspathEntry classpathEntry : classPathEntries) {
				// fix jar files
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
						&& classpathEntry.getPath().equals(jar.getFullPath())) {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * Some extensions have modules that they don't need and can cause compilation
	 * errors (e.g. hmc modules)
	 */
	private void fixModules(final IProgressMonitor monitor, final ExtensionHolder extension) {
		if ("eventtrackingwsaddon".equals(extension.getName())) {
			extension.setHmcModule(false);
			FixProjectsUtils.updateExtensionModules(extension, monitor);
		}
		// remove broken wst settings file
		if (BROKEN_WST_SETTINGS_FILE_EXT.equals(extension.getName())) {
			final java.nio.file.Path brokenConfig = Paths.get(extension.getPath(), BROKEN_WST_SETTINGS_FILE);
			if (Files.exists(brokenConfig)) {
				try {
					Files.delete(brokenConfig);
				} catch (final IOException e) {
					Activator.logError("couldn't delete broken wst config file", e);
				}
			}
		}
	}

}
