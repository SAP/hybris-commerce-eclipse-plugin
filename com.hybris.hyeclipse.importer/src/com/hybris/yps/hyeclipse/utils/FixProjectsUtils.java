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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.HybrisUtil;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;

public class FixProjectsUtils {
	private static final String CONFIG_DIR = "config";
	private static Activator plugin = Activator.getDefault();
	private static final boolean DEBUG = plugin.isDebugging();
	
	private FixProjectsUtils() {
		// hiding implicit constructor
	}
	
	public static Set<ExtensionHolder> getAllExtensionsForPlatform() {
		return plugin.getAllExtensionsForPlatform();
	}
	
	public static String getConfigDirectory()
	{
		return plugin.getConfigDirectory();
	}
	
	public static Set<IProject> getProjectsNotInLocalExtensionsFile() {
		Set<ExtensionHolder> exts = getAllExtensionsForPlatform();
		Set<IProject> allHybrisProjects = getAllHybrisProjects();

		Set<IProject> projectsNotInLocalExts = new HashSet<>();
		for (IProject project : allHybrisProjects) {
			boolean found = false;
			for (ExtensionHolder ext : exts) {

				Path projectLocation = Paths.get(project.getLocation().toFile().getAbsolutePath());
				Path extLocation = Paths.get(ext.getPath());
				try {
					if (Files.isSameFile(projectLocation, extLocation)
							|| (project.getName().equals(Constants.PLATFROM) || (project.getName().equals(CONFIG_DIR)))) {
						found = true;
						break;
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
			// if we get here there is no path
			if (!found && (!project.getName().equals(Constants.PLATFROM) && !project.getName().equals(CONFIG_DIR))) {
				projectsNotInLocalExts.add(project);
			}
		}
		return projectsNotInLocalExts;
	}
		
	public static Set<ExtensionHolder> getExtensionsNotInWorkspace(String platformHome) {

		Set<ExtensionHolder> exts = getAllExtensionsForPlatform();

		// add platform and config to the list of extensions
		ExtensionHolder platformHolder = new ExtensionHolder(platformHome, Constants.PLATFROM);
		exts.add(platformHolder);
		ExtensionHolder configHolder = new ExtensionHolder(getConfigDirectory(), CONFIG_DIR);
		exts.add(configHolder);

		Set<IProject> allHybrisProjects = getAllHybrisProjects();

		Set<ExtensionHolder> extensionsNotInWorkspace = new HashSet<>();
		for (ExtensionHolder ext : exts) {

			boolean found = false;
			for (IProject project : allHybrisProjects) {
				Path projectLocation = Paths.get(project.getLocation().toFile().getAbsolutePath());
				Path extLocation = Paths.get(ext.getPath());
				try {
					if (Files.isSameFile(projectLocation, extLocation)) {
						// Match in workspace and localextensions.xml: between extension & project
						found = true;
						break;
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
			// if we get here there is no match
			if (!found) {
				if (DEBUG)
					Activator.log(
							"No match in workspace for extension [" + ext.getName() + "] path [" + ext.getPath() + "]");
				extensionsNotInWorkspace.add(ext);
			}
		}

		return extensionsNotInWorkspace;
	}
	
	/**
	 * We don't want to touch any projects that are not hybris extensions so we do some filtering
	 * @return
	 */
	public static Set<IProject> getAllHybrisProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project))
			{
				if (DEBUG)
					Activator.log("hybris project found [" +  project.getName() + "]");
				filteredProjects.add(project);
			}
		}
		return filteredProjects;
	}
	
	public static Set<IProject> getAllOpenHybrisProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project) && project.isOpen())
			{
				if (DEBUG)
					Activator.log("hybris project found [" +  project.getName() + "]");
				filteredProjects.add(project);
			}
		}
		return filteredProjects;
	}
	
	/**
	 * Return true if there is an extensioninfo.xml file in the root of the project directory
	 * @param project
	 * @return
	 */
	public static boolean isAHybrisExtension(IProject project) {
		// handle the 2 special cases
		if (project.getName().equals(Constants.PLATFROM) || project.getName().equals(CONFIG_DIR)) {
			return true;
		}
		else {
			return Files.exists(Paths.get(project.getLocation().toFile().getAbsolutePath(), Constants.EXTENSION_INFO_XML));
		}
	}
	
	public static boolean isAPlatformExtension(IProject project)
	{
		String path = project.getLocation().toFile().getAbsolutePath();
		String binDir = "bin" + File.separator;
		return (path.indexOf(binDir + "ext-") >= 0 || path.indexOf(binDir + Constants.PLATFROM) >= 0);
	}
	
	public static boolean isATemplateExtension(IProject project)
	{
		return project.getLocation().append("extgen.properties").toFile().exists();
	}
	
	public static boolean isACustomerExtension(IProject project)
	{
		return !isAPlatformExtension(project) && !project.getName().equals(CONFIG_DIR);
	}
	
	public static Set<IProject> getAllPlatformProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project) && isAPlatformExtension(project))
			{
				filteredProjects.add(project);
			}
		}
		return filteredProjects;
	}
	
	public static Set<IProject> getAllCustomerProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project) && isACustomerExtension(project))
			{
				filteredProjects.add(project);
			}
		}
		return filteredProjects;
	}
	
	public static void addSourceDirectoriesIfExistingForDir(IProgressMonitor monitor, IProject project,
			IJavaProject javaProject, String dir) throws JavaModelException {

		if (project.getFolder(dir).exists()
				&& (!isOnClasspath(project.getFolder(dir), javaProject, IClasspathEntry.CPE_SOURCE))) {
			// check that we don't have the project on the class-path as a source extension
			if (isOnClasspath(project, javaProject, IClasspathEntry.CPE_SOURCE)) {
				removeFromClassPath(project, javaProject, monitor);
			}
			addToClassPath(project.getFolder(dir), IClasspathEntry.CPE_SOURCE, javaProject, monitor);
		}
	}
	
	/**
	 * Add *all* the *currently existing* sources directories to the project.
	 * They will be pruned later in updateExtensionModules().
	 */
	public static void addSourceDirectoriesIfExisting(IProgressMonitor monitor, IProject project, IJavaProject javaProject) throws JavaModelException {
		// fix sources directories (if src and gensrc exist in the project then add then)
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/src");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/gensrc");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/testsrc");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/web/src");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/hmc/src");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/web/testsrc");
		addSourceDirectoriesIfExistingForDir(monitor, project, javaProject, "/acceleratoraddon/web/src");
	}
	
	/**
	 * Using IJavaProject.isOnClasspath() is not accurate
	 * 
	 * @throws JavaModelException
	 */
	private static boolean isOnClasspath(IResource resource, IJavaProject javaProject, int type)
			throws JavaModelException {
		for (IClasspathEntry classPath : javaProject.getRawClasspath()) {
			if ((classPath.getEntryKind() == type) && resource.getFullPath().equals(classPath.getPath())) {
				return true;
			}
		}
		return false;
	}
	
	private static void removeSourceDirectoryIfNotExsistingForDir(IProgressMonitor monitor, IProject project,
			IJavaProject javaProject, String dir) throws JavaModelException {

		if (!project.getFolder(dir).exists() && javaProject.isOnClasspath(project.getFolder(dir))) {
			removeFromClassPath(project.getFolder(dir), javaProject, monitor);
		}
	}
	
	public static void removeSourceDirectoriesIfNotExisting(IProgressMonitor monitor, IProject project, IJavaProject javaProject) throws JavaModelException {
			
		// fix sources directories (if src and gensrc exist in the project then add then)
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/src");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/gensrc");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/testsrc");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/acceleratoraddon/web/src");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/acceleratoraddon/web/webroot/WEB-INF/tags");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/acceleratoraddon/web/webroot/WEB-INF/views");
		removeSourceDirectoryIfNotExsistingForDir(monitor, project, javaProject, "/acceleratoraddon/web/webroot/WEB-INF/tld");
	}
	
	public static void removeFromClassPath(IResource res, IJavaProject javaProject, IProgressMonitor monitor)
			throws JavaModelException {
		List<IClasspathEntry> entries = new LinkedList<>(Arrays.asList(javaProject.getRawClasspath()));

		ListIterator<IClasspathEntry> iterator = entries.listIterator();
		boolean changed = false;
		while (iterator.hasNext()) {
			IClasspathEntry entry = iterator.next();
			if ((entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) && res.getFullPath().equals(entry.getPath())) {
				changed = true;
				if (DEBUG)
					Activator.log("Removing src path [" + res.getFullPath() + "] for project ["
							+ javaProject.getProject().getName() + "]");
				iterator.remove();
			}
		}

		if (changed) {
			setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject, monitor);
		}
	}
	
	public static void addToClassPath(IResource res, int type, IJavaProject javaProject, IProgressMonitor monitor) throws JavaModelException
	{
		Set<IClasspathEntry> entries = new HashSet<>(Arrays.asList(javaProject.getRawClasspath()));
		IClasspathEntry entry = null;
		switch (type) {
		case IClasspathEntry.CPE_LIBRARY: 
			entry = JavaCore.newLibraryEntry(res.getFullPath(), null, null, true);
			break;
		case IClasspathEntry.CPE_PROJECT: 
			entry = JavaCore.newProjectEntry(res.getFullPath(), true);
			break;
		case IClasspathEntry.CPE_SOURCE:
		default:
			entry = JavaCore.newSourceEntry(res.getFullPath());
			break;
		}
			
		entries.add(entry);
		setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject, monitor);
	}
	
	public static void setClasspath(IClasspathEntry[] classpath, IJavaProject javaProject, IProgressMonitor monitor) throws JavaModelException
	{
		// backup .classpath
		File classPathFileBak = javaProject.getProject().getFile(".classpath.bak").getLocation().toFile();
		if (!classPathFileBak.exists())
		{
			File classPathFile = javaProject.getProject().getFile(".classpath").getLocation().toFile();
			try {
				Files.copy(Paths.get(classPathFile.getAbsolutePath()), Paths.get(classPathFileBak.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				// can't back up file but we should continue anyway
				Activator.log("Failed to backup classfile [" + classPathFile.getAbsolutePath() + "]");
			}
		}
		javaProject.setRawClasspath(classpath, monitor);	
	}
	

	public static void removeBuildersFromProject(IProgressMonitor monitor, IProject project) throws CoreException
	{
		final IProjectDescription description = project.getDescription();
		// remove hybris builder
		for (ICommand build : project.getDescription().getBuildSpec())
		{
			if (build.getBuilderName().equals("org.eclipse.jdt.core.javabuilder"))
			{
				description.setBuildSpec(new ICommand[] { build });
				project.setDescription(description, monitor);
				break;
			}
		}	
	}

	public static void updateExtensionModules(final ExtensionHolder extension, IProgressMonitor monitor)
			throws TransformerFactoryConfigurationError {
		monitor.beginTask("Removing module info", 10);
		String extensionPath = extension.getPath();
		
		if (HybrisUtil.isHybrisModuleRoot(new File(extensionPath))) {
			try {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(extension.getName());
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				docFactory.setValidating(true);
				docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				docBuilder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
				
				File extInfo = new File(extensionPath, Constants.EXTENSION_INFO_XML);
				Document doc = docBuilder.parse(extInfo);
				boolean updateProject = false;
				if (!extension.isCoreModule()) {
					String moduleName = "coremodule";
					removeAndAddComment(doc, moduleName);
					
					//Remove 'src' directory
					removeSourceFolder(monitor, project, "src", "src");
					//Remove 'gensrc' directory
					removeSourceFolder(monitor, project, "gensrc", "gensrc");
					//Remove 'testsrc' directory
					removeSourceFolder(monitor, project, "testsrc", "testsrc");
					
					//Remove *-items.xml, *-spring.xml
					project.getFile("resources/" + extension.getName() + "-spring.xml").delete(true, false, monitor);
					project.getFile("resources/" + extension.getName() + "-items.xml").delete(true, false, monitor);
					
					updateProject = true;
				}
				if (!extension.isWebModule()) {
					String moduleName = "webmodule";
					removeAndAddComment(doc, moduleName);
					
					//Remove 'web' directory
					removeSourceFolder(monitor, project, "web", "web/src");
					removeSourceFolder(monitor, project, "web", "web/testsrc");

					updateProject = true;
				}
				if (!extension.isHmcModule()) {
					String moduleName = "hmcmodule";
					removeAndAddComment(doc, moduleName);
					
					//Remove 'hmc' directory
					removeSourceFolder(monitor, project, "hmc", "hmc/src");
					
					updateProject = true;
				}
				if (updateProject) {
					// write the content into xml file
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
					transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
					transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(new File(extensionPath, Constants.EXTENSION_INFO_XML));
					transformer.transform(source, result);
					monitor.worked(5);
					
					//Refresh local project
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			}
			catch (ParserConfigurationException| TransformerException | IOException |SAXException| CoreException pce) {
				Activator.logError("InvocationTargetException", pce);
			}
			
		}
		monitor.worked(5);
		monitor.done();
	}
	
	private static void removeSourceFolder(IProgressMonitor monitor, IProject project, String folderName, String classpathEntry)
			throws CoreException {
		IFolder webFolder = project.getFolder(folderName);
		if(webFolder != null){
			webFolder.delete(true, false, monitor);
		}
		//Project is missing required source folder: 'web/src'
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject.isOnClasspath(project.getFolder(classpathEntry)))
		{
			FixProjectsUtils.removeFromClassPath(project.getFolder(classpathEntry), javaProject, monitor);
		}
	}

	private static void removeAndAddComment(Document doc, String moduleName) {
		Node extensionNode;
		Node moduleNode;
		moduleNode = doc.getElementsByTagName(moduleName).item(0);
		if (moduleNode != null) {
			extensionNode = moduleNode.getParentNode();
			NamedNodeMap attrs = moduleNode.getAttributes();
			StringBuilder sb = new StringBuilder("<");
			sb.append(moduleName);
			for (int i = 0 ; i<attrs.getLength() ; i++) {
		        Attr attribute = (Attr)attrs.item(i);     
		        sb.append(" " + attribute.getName() + "=\"" + attribute.getValue() + "\"");
		    }
			sb.append("/>");
			
			appendXmlFragment(extensionNode, sb.toString());
			extensionNode.removeChild(moduleNode);
		}
	}
	
	private static void appendXmlFragment(Node parent, String fragment){
		Document doc = parent.getOwnerDocument();
		Comment comment = doc.createComment(fragment);
		parent.appendChild(comment);
	}

	public static void setOutputDirectory(IProgressMonitor monitor, IProject project, IJavaProject javaProject) {

		IPath outputLocation = null;
		IPath newOutputLocation = null;
		try {
			outputLocation = javaProject.getOutputLocation();

			// Only change the output location if it is the eclipse default one "bin"
			if ("bin".equals(outputLocation.lastSegment())) {
				newOutputLocation = outputLocation.removeLastSegments(1).append("eclipsebin");
				javaProject.setOutputLocation(newOutputLocation, monitor);
			}
		} catch (JavaModelException e) {
			Activator.logError(String.format("Could not set output directory %s for project %s", outputLocation, project.getName()), e);
		}
	}
}
