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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
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

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;

public class Importer {

	private static Activator plugin = Activator.getDefault();
	private static final boolean DEBUG = Activator.getDefault().isDebugging();

	private static final String HYBRIS_NATURE_ID = "com.hybris.hyeclipse.tsv.hybris";
	private static final String SPRING_NATURE_ID = "org.springframework.ide.eclipse.core.springnature";
	private static final String SETTINGS_FILE = ".settings/org.eclipse.jdt.core.prefs";
	private static final String SPRINGBEANS_FILE = ".springBeans";
	private static final String HYBRIS_EXTENSION_FILE = "extensioninfo.xml";

	private static final double JVM8_VERSION = 5.6d;
	private static final double JVM11_VERSION = 1811d;

	public void resetProjectsFromLocalExtensions(File platformHome, IProgressMonitor monitor, boolean fixClasspath,
			boolean removeHybrisGenerator, boolean createWorkingSets, boolean useMultiThread, boolean skipJarScanning)
			throws CoreException {
		plugin.resetPlatform(platformHome.getAbsolutePath());

		importExtensionsNotInWorkspace(monitor, platformHome);
		closeProjectsThatAreNotReferenced(monitor, platformHome);

		if (fixClasspath) {
			fixMissingProjectDependencies(monitor, platformHome);
			fixMissingProjectResources(monitor, platformHome);
			fixProjectClasspaths(monitor, platformHome);
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

		fixSpringBeans(monitor, platformHome);
	}

	private void importExtensionsNotInWorkspace(IProgressMonitor monitor, File platformHome) throws CoreException {
		Activator.log("Retrieving extensions not in workspace");

		double version = getPlatformVersion(platformHome);
		
		Collection<ExtensionHolder> extensions = FixProjectsUtils
				.getExtensionsNotInWorkspace(platformHome.getAbsolutePath());
		if (!extensions.isEmpty()) {
			monitor.setTaskName("Importing extensions");
			monitor.beginTask("Importing extensions", extensions.size());
			int progress = 0;
			for (ExtensionHolder extensionHolder : extensions) {
				IPath path = new Path(extensionHolder.getPath()).append("/.project");
				if (path.toFile().exists() || isHybrisExtension(path)) {
					Activator.log("Importing project [" + extensionHolder + "]");
					importProject(monitor, path, version);
					// fix the modules (e.g. remove hmc module if not needed)
					fixModules(monitor, extensionHolder);
				} else {
					// TODO fix skipping eclipse projects. create them and add to workspace
					if (DEBUG)
						Activator.log(MessageFormat.format(
								"Not importing extension [{0}] because it is not an eclipse project", extensionHolder));
				}
				progress++;
				monitor.worked(progress);
			}
		}
	}

	/**
	 * Method checks if given folder is SAP Commerce extension.
	 * 
	 * @param path to folder found by import process as an extension
	 * @return <code>true</code> if folder contains
	 *         {@code Importer#HYBRIS_EXTENSION_FILE}
	 */
	protected boolean isHybrisExtension(IPath path) {
		return path.append(HYBRIS_EXTENSION_FILE).toFile().exists();
	}

	private void closeProjectsThatAreNotReferenced(IProgressMonitor monitor, File platformHome) throws CoreException {
		if (DEBUG)
			Activator.log("Retrieving projects not in localextensions using platformhome ["
					+ platformHome.getAbsolutePath() + "]");
		Set<IProject> projectsToClose = FixProjectsUtils
				.getProjectsNotInLocalExtensionsFile(platformHome.getAbsolutePath());

		// close projects from the above set that are not referenced by a
		// project that is not scheduled for closing
		if (projectsToClose != null) {
			for (IProject projectToClose : projectsToClose) {

				// check if this project is a dependency of another project that
				// is not scheduled to be closed
				IProject[] referencingProjects = projectToClose.getReferencingProjects();
				boolean abortClose = false;
				if (referencingProjects != null) {
					for (IProject proj : referencingProjects) {
						if (!projectsToClose.contains(proj)) {
							if (DEBUG)
								Activator.log("Aborting close of project [" + projectToClose.getName()
										+ "] because it is referenced by [" + proj.getName() + "]");
							abortClose = true;
						}
					}
				}

				// close projects
				if (!abortClose) {
					if (DEBUG)
						Activator.log("Closing project [" + projectToClose.getName() + "]");
					projectToClose.close(monitor);
				}
			}
		}
	}

	private IProject importProject(IProgressMonitor monitor, IPath path, double version) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, 120);
		IProjectDescription description = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, progress.newChild(30));
		fixProjectCompilerSettings(monitor, project, version);
		project.open(progress.newChild(30));
		FixProjectsUtils.removeBuildersFromProject(progress.newChild(30), project);
		addHybrisNature(project, progress.newChild(30));
		return project;
	}

	private double getPlatformVersion(File platformHome) {
		java.nio.file.Path buildNumberPath = platformHome.toPath().resolve("build.number");
		Double platformVersion = 6.3d;
		try (FileReader fr = new FileReader(buildNumberPath.toFile())) {
			Properties platformProps = new Properties();
			platformProps.load(fr);
			String propertyVersion = platformProps.getProperty("version", "6.3");
			platformVersion = Double.valueOf(propertyVersion);
		} catch (IOException e) {
			throw new IllegalStateException(MessageFormat.format("Error reading file {0}", buildNumberPath), e);
		}
		return platformVersion;
	}

	private void fixProjectCompilerSettings(IProgressMonitor monitor, IProject project, double platformVersion) {
		// don't fix project or custom extensions
		if (FixProjectsUtils.isAPlatformExtension(project) && !FixProjectsUtils.isATemplateExtension(project)) {
			List<String> compileProblems = Arrays.asList("org.eclipse.jdt.core.compiler.problem.autoboxing",
					"org.eclipse.jdt.core.compiler.problem.emptyStatement",
					"org.eclipse.jdt.core.compiler.problem.unusedLocal",
					"org.eclipse.jdt.core.compiler.problem.unnecessaryTypeCheck",
					"org.eclipse.jdt.core.compiler.problem.undocumentedEmptyBlock");

			File settingsFile = new File(project.getLocation() + "/" + (SETTINGS_FILE));
			if (settingsFile.exists()) {

				String s;
				final StringBuilder strBuilder = new StringBuilder();
				boolean fileHasBeenModified = false;
				try (FileReader fr = new FileReader(settingsFile); BufferedReader br = new BufferedReader(fr);) {
					while ((s = br.readLine()) != null) {
						boolean thisLineChanged = false;
						for (String compileProblem : compileProblems) {
							if (s.startsWith(compileProblem)) {
								if (s.endsWith("error")) {
									fileHasBeenModified = true;
									thisLineChanged = true;
									strBuilder.append(s.replaceAll("error", "warning")).append("\n");
									break;
								}
							}
							// make sure all 1.7 settins are substitute for 1.8
							// for versions 5.6 and higher
							if (platformVersion >= JVM8_VERSION && s.indexOf("1.7") > 0) {
								fileHasBeenModified = true;
								thisLineChanged = true;
								strBuilder.append(s.replaceAll("1\\.7", "1\\.8")).append("\n");
								break;
							}
							if (platformVersion >= JVM11_VERSION) {
								fileHasBeenModified = true;
								thisLineChanged = true;
								strBuilder.append(s.replaceAll("1\\.(7|8)", "11")).append("\n");
								break;
							}
						}
						if (!thisLineChanged) {
							strBuilder.append(s).append("\n");
						}
					}

				} catch (IOException e) {
					throw new IllegalStateException(
							"Error while fixing the compiler settings in " + settingsFile.toString(), e);
				}

				// write the settings if they have changed
				if (fileHasBeenModified) {
					try (FileWriter fw = new FileWriter(settingsFile)) {
						fw.write(strBuilder.toString());
					} catch (IOException e) {
						throw new IllegalStateException(
								"Error while fixing the compiler settings in " + settingsFile.toString(), e);
					}
				}

			}
		}
	}

	private void addHybrisNature(IProject project, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (HYBRIS_NATURE_ID.equals(natures[i])) {
				return;
			}
		}

		// Add the nature
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = HYBRIS_NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, monitor);
	}

	private void fixBuilders(IProgressMonitor monitor) throws CoreException {
		monitor.setTaskName("Fixing builders");
		Set<IProject> hybrisProjects = FixProjectsUtils.getAllOpenHybrisProjects();
		monitor.beginTask("Fixing builders", hybrisProjects.size());
		int i = 0;
		for (IProject project : hybrisProjects) {
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
	private void fixProjectClasspaths(IProgressMonitor monitor, File platformHome) {
		if (ResourcesPlugin.getWorkspace().getRoot().getProjects() == null) {
			return;
		}

		monitor.setTaskName("Fixing project classpaths");
		monitor.beginTask("Fixing project classpaths", ResourcesPlugin.getWorkspace().getRoot().getProjects().length);
		monitor.worked(0);
		int projCnt = 0;
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			{
				try {
					if (FixProjectsUtils.isAHybrisExtension(project) && project.isOpen()
							&& project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject javaProject = JavaCore.create(project);
						removeJarFilesThatDontExist(monitor, project, javaProject);
						addJarFilesNotInClasspath(monitor, project, javaProject);
						FixProjectsUtils.addSourceDirectoriesIfExisting(monitor, project, javaProject);
						FixProjectsUtils.removeSourceDirectoriesIfNotExisting(monitor, project, javaProject);
						FixProjectsUtils.setOutputDirectory(monitor, project, javaProject);
						fixBackofficeJars(monitor, javaProject);
						fixAddons(monitor, javaProject);
						fixMissingJavaRuntime(monitor, javaProject);
					}
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
			}
			projCnt++;
			monitor.worked(projCnt);
		}
	}

	private void fixSpringBeans(IProgressMonitor monitor, File platformHome) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		if (projects == null) {
			return;
		}

		monitor.setTaskName("Fixing Spring Beans");
		monitor.beginTask("Fixing Spring Beans", projects.length);
		monitor.worked(0);
		int projCnt = 0;
		for (IProject project : projects) {
			monitor.worked(projCnt++);

			try {

				if (!project.isOpen()) {
					continue;
				}

				if (!FixProjectsUtils.isAHybrisExtension(project)) {
					continue;
				}

				if (!project.hasNature(SPRING_NATURE_ID)) {
					continue;
				}

				IPath location = project.getLocation();

				File springBeansFile = new File(location + "/" + (SPRINGBEANS_FILE));
				// rewriting the lines between : <configs> & </configs>
				// It is very crude, but it works since the file should be
				// formatted nicely.
				File springBeansFileNew = new File(location + "/" + (SPRINGBEANS_FILE) + ".new");

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
					String[] beansFiles = getAllSpringXmlFiles(project);

					fw.write("\t<configs>\n");
					for (String beansFile : beansFiles) {
						File beanFile = new File(location + "/" + beansFile);
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

				} catch (IOException e) {
					throw new IllegalStateException(
							"Error while fixing the compiler settings in " + springBeansFile.toString(), e);
				}

				springBeansFileNew.renameTo(springBeansFile);

			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}

		}

	}

	private String[] getAllSpringXmlFiles(IProject project) {
		String projectName = project.getName();
		String[] allfiles = { //
				"resources/" + projectName + "-spring.xml", //
				"web/webroot/WEB-INF/" + projectName + "-web-spring.xml", //
		};
		return allfiles;
	}

	/**
	 * Sometimes the project configuration is corrupt and a Java runtime is not on
	 * the classpath
	 * 
	 * @param monitor
	 * @param javaProject
	 * @throws JavaModelException
	 */
	private void fixMissingJavaRuntime(IProgressMonitor monitor, IJavaProject javaProject) throws JavaModelException {

		if (!javaProject.getProject().getName().equals("config")) {
			IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
			boolean found = false;
			for (IClasspathEntry classpathEntry : classPathEntries) {
				// fix missing runtime
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					if (classpathEntry.getPath().toString().startsWith("org.eclipse.jdt.launching.JRE_CONTAINER")) {
						found = true;
						break;
					}
				}
			}

			if (!found) {
				IClasspathEntry entry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"),
						false);
				Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>(Arrays.asList(classPathEntries));
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
	private void fixAddons(IProgressMonitor monitor, IJavaProject javaProject) throws JavaModelException {

		final String projectName = javaProject.getProject().getName();

		if (projectName.equals("orderselfserviceaddon") || projectName.equals("notificationaddon")
				|| projectName.equals("customerinterestsaddon") || projectName.equals("consignmenttrackingaddon")) {

			IProject acceleratorstorefrontcommonsProject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject("acceleratorstorefrontcommons");
			if (acceleratorstorefrontcommonsProject != null && acceleratorstorefrontcommonsProject.exists()) {
				if (!javaProject.isOnClasspath(acceleratorstorefrontcommonsProject)) {
					FixProjectsUtils.addToClassPath(acceleratorstorefrontcommonsProject, IClasspathEntry.CPE_PROJECT,
							javaProject, monitor);
				}
			}
		}

		if (projectName.equals("stocknotificationaddon")) {
			IProject notificationfacadesProject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject("notificationfacades");
			if (notificationfacadesProject != null && notificationfacadesProject.exists()) {
				if (!javaProject.isOnClasspath(notificationfacadesProject)) {
					FixProjectsUtils.addToClassPath(notificationfacadesProject, IClasspathEntry.CPE_PROJECT,
							javaProject, monitor);
				}
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
	private void fixBackofficeJars(IProgressMonitor monitor, IJavaProject javaProject) throws JavaModelException {
		if (javaProject.getProject().getName().equalsIgnoreCase("backoffice")) {
			List<IClasspathEntry> entries = new LinkedList<IClasspathEntry>();
			IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
			boolean change = false;
			for (IClasspathEntry classpathEntry : classPathEntries) {
				// fix jar files
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (classpathEntry.getPath().toString()
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
							|| classpathEntry.getPath().toString()
									.contains("/backoffice/web/webroot/WEB-INF/lib/cockpit-")
							|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/zk")
							|| classpathEntry.getPath().toString().contains("/backoffice/web/webroot/WEB-INF/lib/zul-")
							|| classpathEntry.getPath().toString()
									.contains("/backoffice/web/webroot/WEB-INF/lib/zcommon-")) {
						if (!classpathEntry.isExported()) {
							change = true;
							IClasspathEntry clonedEntry = JavaCore.newLibraryEntry(classpathEntry.getPath(),
									classpathEntry.getSourceAttachmentPath(),
									classpathEntry.getSourceAttachmentRootPath(), classpathEntry.getAccessRules(),
									classpathEntry.getExtraAttributes(), true);
							entries.add(clonedEntry);
							continue;
						}
					}
				}
				entries.add(classpathEntry);
			}
			if (change) {
				FixProjectsUtils.setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject,
						monitor);
			}
		}
	}

	private void removeJarFilesThatDontExist(IProgressMonitor monitor, IProject project, IJavaProject javaProject)
			throws JavaModelException {
		IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		List<IClasspathEntry> newClassPathEntries = new LinkedList<IClasspathEntry>();
		boolean changedClassPath = false;
		for (IClasspathEntry classpathEntry : classPathEntries) {
			// fix jar files
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				classpathEntry.getPath();
				File classpathEntryFile = classpathEntry.getPath().toFile();

				// remove JAR if it doesn't exist, only do this if the jar file is located in
				// this project, we leave jars references from different projects
				if (classpathEntryFile.getPath().endsWith(".jar")
						&& classpathEntryFile.getPath().startsWith("/" + project.getName() + "/")
						&& !project.getFile(classpathEntryFile.getPath().replace("/" + project.getName() + "/", "/"))
								.exists()) {
					changedClassPath = true;
					if (DEBUG)
						Activator.log("libary [" + classpathEntry.getPath() + "] not found for project [ "
								+ project.getName() + "]");
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

	private void fixMissingProjectDependencies(IProgressMonitor monitor, File platformHome) throws JavaModelException {
		Set<ExtensionHolder> extensions = FixProjectsUtils.getAllExtensionsForPlatform(platformHome.getAbsolutePath());
		Set<IProject> projects = FixProjectsUtils.getAllOpenHybrisProjects();
		for (IProject project : projects) {
			for (ExtensionHolder extHolder : extensions) {
				if (extHolder.getName().equalsIgnoreCase(project.getName())) {
					addMissingProjectDependencies(monitor, project, extHolder);
					continue;
				}
			}
		}
	}

	private void addMissingProjectDependencies(IProgressMonitor monitor, IProject project, ExtensionHolder extHolder)
			throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(project);

		IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		if (!extHolder.getDependentExtensions().isEmpty()) {
			for (String ext : extHolder.getDependentExtensions()) {
				boolean found = false;
				for (IClasspathEntry classpathEntry : classPathEntries) {
					// fix jar files
					if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
						if (classpathEntry.getPath().toString().replaceFirst("/", "").equalsIgnoreCase(ext)) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					IProject dependentProject = ResourcesPlugin.getWorkspace().getRoot().getProject(ext);
					if (dependentProject.exists() && dependentProject.isOpen()) {
						FixProjectsUtils.addToClassPath(dependentProject, IClasspathEntry.CPE_PROJECT, javaProject,
								monitor);
					}
				}
			}
		}
	}

	private void fixMissingProjectResources(IProgressMonitor monitor, File platformHome) throws CoreException {
		Set<IProject> projects = FixProjectsUtils.getAllHybrisProjects();
		IProject config = null;
		IProject platform = null;
		for (IProject proj : projects) {
			if ("config".equals(proj.getName())) {
				config = proj;
			}
			if ("platform".equals(proj.getName())) {
				platform = proj;
			}
		}
		if (config != null && platform != null) {
			IFile activeRoleEnvPropertyFile = platform.getFile("active-role-env.properties");
			IFile instancePropertiesLink = config.getFile("instance.properties");

			if (instancePropertiesLink.exists()) {
				// Remove the instance.properties link if existing
				instancePropertiesLink.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, monitor);
			}
			if (activeRoleEnvPropertyFile.exists()) {
				// extract currently enabled role & instance from active-role-env.properties
				// https://wiki.hybris.com/display/RD/hybris+server+roles)
				Properties prop = new Properties();
				try (InputStream input = new FileInputStream(activeRoleEnvPropertyFile.getLocation().toFile())) {
					prop.load(input);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
				String activeRole = prop.getProperty("ACTIVE_ROLE").replace("${platformhome}", platformHome.toString());
				String activeInstance = prop.getProperty("ACTIVE_ROLE_INSTANCE").replace("${platformhome}",
						platformHome.toString());

				// Create the instance.properties link
				File hybrisRootDir = platform.getLocation().toFile().getParentFile().getParentFile();
				File instanceConfigDir = new File(
						new File(new File(new File(hybrisRootDir, "roles"), activeRole), activeInstance), "config");
				File instancePropertiesFile = new File(instanceConfigDir, "instance.properties");

				IPath location = new Path(instancePropertiesFile.toString());
				if (DEBUG) {
					Activator.log("location = " + location.toString());
					Activator.log("instancePropertiesLink = " + instancePropertiesLink.toString());
				}

				instancePropertiesLink.createLink(location, IResource.NONE, null);
			}
		}
	}

	private void addJarFilesNotInClasspath(IProgressMonitor monitor, IProject project, IJavaProject javaProject)
			throws CoreException {
		addMembersOfFolderToClasspath("/lib", monitor, javaProject);
		addMembersOfFolderToClasspath("/web/webroot/WEB-INF/lib", monitor, javaProject);
		// check if this is a backoffice extension
		IFolder backofficeFolder = javaProject.getProject().getFolder("/resources/backoffice");
		if (backofficeFolder != null && backofficeFolder.exists()) {
			IResource backofficeJar = backofficeFolder.findMember(javaProject.getProject().getName() + "_bof.jar");
			if (backofficeJar != null && backofficeJar.exists()) {
				if (!isClasspathEntryForJar(javaProject, backofficeJar)) {
					Activator.log("Adding library [" + backofficeJar.getFullPath() + "] to classpath for project ["
							+ javaProject.getProject().getName() + "]");
					FixProjectsUtils.addToClassPath(backofficeJar, IClasspathEntry.CPE_LIBRARY, javaProject, monitor);
				}
			}
		}

		// add db drivers for platform/lib/dbdriver directory
		if (project.getName().equalsIgnoreCase("platform")) {
			addMembersOfFolderToClasspath("/lib/dbdriver", monitor, javaProject);
		}
	}

	private void addMembersOfFolderToClasspath(String path, IProgressMonitor monitor, IJavaProject javaProject)
			throws CoreException, JavaModelException {
		IFolder folder = javaProject.getProject().getFolder(path);
		if (folder != null && folder.exists()) {
			for (IResource res : folder.members()) {
				if (res.getFileExtension() != null && res.getFileExtension().equals("jar") && res.exists()) {
					// check if this Resource is on the classpath
					if (!javaProject.isOnClasspath(res)) {
						if (DEBUG)
							Activator.log("Adding library [" + res.getFullPath() + "] to classpath for project ["
									+ javaProject.getProject().getName() + "]");
						FixProjectsUtils.addToClassPath(res, IClasspathEntry.CPE_LIBRARY, javaProject, monitor);
					}
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
	private boolean isClasspathEntryForJar(IJavaProject javaProject, IResource jar) throws JavaModelException {
		IClasspathEntry[] classPathEntries = javaProject.getRawClasspath();
		if (classPathEntries != null) {
			for (IClasspathEntry classpathEntry : classPathEntries) {
				// fix jar files
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (classpathEntry.getPath().equals(jar.getFullPath())) {
						return true;
					}

				}
			}

		}
		return false;
	}

	/**
	 * Some extensions have modules that they don't need and can cause compilation
	 * errors (e.g. hmc modules)
	 */
	private void fixModules(IProgressMonitor monitor, ExtensionHolder extension) throws JavaModelException {

		if (extension.getName().equals("eventtrackingwsaddon")) {
			extension.setHmcModule(false);
			FixProjectsUtils.updateExtensionModules(extension, monitor);
		}
	}

}
