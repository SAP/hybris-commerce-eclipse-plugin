package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
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

import org.apache.tools.ant.util.StringUtils;
import org.eclipse.core.resources.ICommand;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;

public class FixProjectsUtils {
	
	private static Activator plugin = Activator.getDefault();
	private static final boolean debug = plugin.isDebugging();

	private static final String CONFIG = "config";
	private static final String PLATFORM = "platform";
	private static final String LOCAL_EXTENSIONS = "localextensions.xml";
	
	
	public static Set<ExtensionHolder> getAllExtensionsForPlatform(String platformHome) {
		Set<ExtensionHolder> allExtensions  = plugin.getAllExtensionsForPlatform(platformHome);
		return allExtensions;
	}
	
	public static String getConfigDirectory()
	{
		return plugin.getConfigDirectory();
	}
	
	public static Set<IProject> getProjectsNotInLocalExtensionsFile(String platformHome)
	{
		Set<ExtensionHolder> exts = getAllExtensionsForPlatform(platformHome);
		Set<IProject> allHybrisProjects = getAllHybrisProjects();
		
		Set<IProject> projectsNotInLocalExts = new HashSet<IProject>();
		if (debug)
			Activator.log("Getting Projects not in localextensions.xml");
		for (IProject project : allHybrisProjects)
		{
			boolean found = false;
			for (ExtensionHolder ext : exts)
			{
				
				Path projectLocation = Paths.get(project.getLocation().toFile().getAbsolutePath());
				Path extLocation = Paths.get(ext.getPath());
				try
				{
				if (Files.isSameFile(projectLocation, extLocation) || (project.getName().equals("platform") || (project.getName().equals("config"))))
				{
					if (debug)
						Activator.log("Ext in workspace and localextensions.xml: ext [" + ext.getPath() + "] project [" + project.getLocation().toFile().getAbsolutePath() + "]");
					found = true;
					break;
				}
				} catch (IOException e)
				{
					throw new IllegalStateException(e);
				}
			}
			// if we get here there is no path
			if (!found)
			{
				if (debug)
					Activator.log("No match in localextensions for project [" + project.getName() + "] path [" + project.getLocation().toFile().getAbsolutePath() + "]");
				if (!project.getName().equals("platform") && !project.getName().equals("config")) {
				projectsNotInLocalExts.add(project);
			}
		}
		}
		return projectsNotInLocalExts;
	}
	
	public static Set<ExtensionHolder> getExtensionsNotInWorkspace(String platformHome) {

		Set<ExtensionHolder> exts = getAllExtensionsForPlatform(platformHome);
		
		// add platform and config to the list of extensions
		ExtensionHolder platformHolder = new ExtensionHolder(platformHome,"platform");
		exts.add(platformHolder);
		ExtensionHolder configHolder = new ExtensionHolder(getConfigDirectory(),"config");
		exts.add(configHolder);
		
		Set<IProject> allHybrisProjects = getAllHybrisProjects();

		Set<ExtensionHolder> extensionsNotInWorkspace = new HashSet<ExtensionHolder>();
		if (debug)
			Activator.log("Getting extensions not in workspace");
		for (ExtensionHolder ext : exts) {
			
			boolean found = false;
			for (IProject project : allHybrisProjects) {
				if (debug)
					Activator.log("ext [" + ext.getPath() + "] project [" + project.getLocation().toFile().getAbsolutePath() + "]");
				
				Path projectLocation = Paths.get(project.getLocation().toFile().getAbsolutePath());
				Path extLocation = Paths.get(ext.getPath());
				try
				{
					if (Files.isSameFile(projectLocation, extLocation))
					{
						if (debug)
							Activator.log("Match in workspace and localextensions.xml: ext [" + ext.getPath() + "] project [" + project.getLocation().toFile().getAbsolutePath() + "]");
						found = true;
						break;
					}
				}
				catch (IOException e)
				{
					throw new IllegalStateException(e);
				}
			}
			// if we get here there is no match
			if (!found)
			{
				if (debug)
					Activator.log("No match in workspace for extension [" + ext.getName() + "] path [" + ext.getPath() + "]");
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
		final Set<IProject> filteredProjects = new HashSet<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project))
			{
				if (debug)
					Activator.log("hybris project found [" +  project.getName() + "]");
				filteredProjects.add(project);
			}
		}
		return filteredProjects;
	}
	
	public static Set<IProject> getAllOpenHybrisProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<IProject>();
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (isAHybrisExtension(project) && project.isOpen())
			{
				if (debug)
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
		if (project.getName().equals("platform") || project.getName().equals("config")) {
			return true;
		}
		else {
			String extensionInfoPath = project.getLocation().toFile().getAbsolutePath() + "/extensioninfo.xml";
			return new File(extensionInfoPath).exists();
		}
	}
	
	public static boolean isAPlatformExtension(IProject project)
	{
		String path = project.getLocation().toFile().getAbsolutePath();
		String binDir = "bin" + File.separator;
		return (path.indexOf(binDir + "ext-") > 0 || path.indexOf(binDir + "platform") > 0);
	}
	
	public static boolean isATemplateExtension(IProject project)
	{
		return project.getLocation().toFile().getAbsolutePath().indexOf("ext-template") > 0;
	}
	
	public static boolean isACustomerExtension(IProject project)
	{
		return !isAPlatformExtension(project) && !project.getName().equals("config");
	}
	
	public static Set<IProject> getAllPlatformProjects()
	{
		final Set<IProject> filteredProjects = new HashSet<IProject>();
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
		final Set<IProject> filteredProjects = new HashSet<IProject>();
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
	
	public static void addSourceDirectoriesIfExistingForDir(IProgressMonitor monitor, IProject project, IJavaProject javaProject, String dir) throws JavaModelException {
		
		if (project.getFolder(dir).exists())
		{
			if (!isOnClasspath(project.getFolder(dir),javaProject, IClasspathEntry.CPE_SOURCE))
			{
				// check that we don't have the project on the classpath as a source extension
				if (isOnClasspath(project,javaProject, IClasspathEntry.CPE_SOURCE))
				{
					removeFromClassPath(project, IClasspathEntry.CPE_SOURCE, javaProject, monitor);
				}
				addToClassPath(project.getFolder(dir), IClasspathEntry.CPE_SOURCE, javaProject, monitor);
			}			
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
	 * @throws JavaModelException 
	 */
	private static boolean isOnClasspath(IResource resource, IJavaProject javaProject, int type) throws JavaModelException
	{
		for (IClasspathEntry classPath : javaProject.getRawClasspath())
		{	
			if (classPath.getEntryKind() ==  type)
			{
				if (resource.getFullPath().equals(classPath.getPath()))
				{
					return true;
				}
			}
		}
		return false;							
	}
	
	private static void removeSourceDirectoryIfNotExsistingForDir(IProgressMonitor monitor, IProject project, IJavaProject javaProject, String dir) throws JavaModelException {
		
		if (!project.getFolder(dir).exists())
		{
			if (javaProject.isOnClasspath(project.getFolder(dir)))
			{
				removeFromClassPath(project.getFolder(dir), IClasspathEntry.CPE_SOURCE, javaProject, monitor);
			}				
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
	
	public static void removeFromClassPath(IResource res, int type, IJavaProject javaProject, IProgressMonitor monitor) throws JavaModelException
	{
		List<IClasspathEntry> entries = new LinkedList<IClasspathEntry>(Arrays.asList(javaProject.getRawClasspath()));
		
		ListIterator<IClasspathEntry> iterator = entries.listIterator();
		boolean changed = false;
		while (iterator.hasNext())
		{
			IClasspathEntry entry = iterator.next();
			if (entry.getEntryKind() ==  IClasspathEntry.CPE_SOURCE)
			{
				if (res.getFullPath().equals(entry.getPath()))
				{
					changed = true;
					if (debug)
						Activator.log("Removing src path [" + res.getFullPath() + "] for project [" + javaProject.getProject().getName() + "]");
					iterator.remove();
				}
			}
		}
		
		if (changed) {
			setClasspath(entries.toArray(new IClasspathEntry[entries.size()]), javaProject, monitor);
		}
	}
	
	public static void addToClassPath(IResource res, int type, IJavaProject javaProject, IProgressMonitor monitor) throws JavaModelException
	{
		Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>(Arrays.asList(javaProject.getRawClasspath()));
		IClasspathEntry entry = null;
		switch (type) {
		case IClasspathEntry.CPE_SOURCE: 
			entry = JavaCore.newSourceEntry(res.getFullPath());
			break;
		case IClasspathEntry.CPE_LIBRARY: 
			entry = JavaCore.newLibraryEntry(res.getFullPath(), null, null, true);
			break;
		case IClasspathEntry.CPE_PROJECT: 
			entry = JavaCore.newProjectEntry(res.getFullPath(), true);
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
		
		File extInfo = new File(extensionPath, "extensioninfo.xml");
		if (extInfo.exists()) {
			try {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(extension.getName());
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				docFactory.setValidating(true);
				docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				docBuilder.setErrorHandler(new ErrorHandler(){
					@Override
				    public void fatalError(SAXParseException exception) throws SAXException
				    {}

				    @Override
				    public void error(SAXParseException exception) throws SAXException
				    {}

				    @Override
				    public void warning(SAXParseException exception) throws SAXException
				    {}
				});
				
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
					StreamResult result = new StreamResult(new File(extensionPath, "extensioninfo.xml"));
					transformer.transform(source, result);
					monitor.worked(5);
					
					//Refresh local project
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			}
			catch (ParserConfigurationException pce) {
				Activator.logError("InvocationTargetException", pce);
			}
			catch (TransformerException tfe) {
				Activator.logError("InvocationTargetException", tfe);
			}
			catch (IOException ioe) {
				Activator.logError("InvocationTargetException", ioe);
			}
			catch (SAXException sae) {
				Activator.logError("InvocationTargetException", sae);
			}
			catch (CoreException e) {
				Activator.logError("InvocationTargetException", e);
			}
			
		}
		monitor.worked(5);
		monitor.done();
	}
	
	private static void removeSourceFolder(IProgressMonitor monitor, IProject project, String folderName, String classpathEntry)
			throws CoreException, JavaModelException {
		IFolder webFolder = project.getFolder(folderName);
		if(webFolder != null){
			webFolder.delete(true, false, monitor);
		}
		//Project is missing required source folder: 'web/src'
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject.isOnClasspath(project.getFolder(classpathEntry)))
		{
			FixProjectsUtils.removeFromClassPath(project.getFolder(classpathEntry), IClasspathEntry.CPE_SOURCE, javaProject, monitor);
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

		IPath outputLocation = null, newOutputLocation = null;
		try {
			outputLocation = javaProject.getOutputLocation();

			// Only change the output location if it is the eclipse default one "bin"
			if ("bin".equals(outputLocation.lastSegment())) {
				newOutputLocation = outputLocation.removeLastSegments(1).append("eclipsebin");
				javaProject.setOutputLocation(newOutputLocation, monitor);
			}
		} catch (JavaModelException e) {
			System.err.println("project:" + project.getName());
			System.err.println("outputLocation:" + outputLocation);
			System.err.println("newOutputLocation:" + newOutputLocation);
			Activator.logError("Could not set output directory", e);
		}
	}
	
	
	public static void updateLocalExtensions(List<IProject> createdProjects) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, TransformerException {
		// Get configPath = workspace/hybris-commerce/hybris/config 
		IPath configPath = getConfigPath();
		if (configPath == null) {
			return;
		}
		
		String localExtensionsPath = configPath.toString() + "/" + LOCAL_EXTENSIONS;
		FileInputStream localExtensionsFileInputStream = new FileInputStream(localExtensionsPath);
		
		// DocumentBuilderFactory & DocumentBuilder
		DocumentBuilderFactory documentBuilderFactory = newDocumentBuilderFactory();
		DocumentBuilder documentBuilder = newDocumentBuilder(documentBuilderFactory);
		
		// Xml Document
		Document xmlDocument = documentBuilder.parse(localExtensionsFileInputStream);
		localExtensionsFileInputStream.close();
		
		// XPath: Find the extensions node.
		XPath xPath = XPathFactory.newInstance().newXPath();
		String extensionsExpression = "/hybrisconfig/extensions[last()]";
		Node extensionsNode = (Node) xPath.evaluate(extensionsExpression, xmlDocument, XPathConstants.NODE);
		
		// Insert extension element(s).
		for(IProject project : createdProjects) {
			Element extensionElement = xmlDocument.createElement("extension");
			extensionElement.setAttribute("name", project.getName());
			extensionsNode.appendChild(extensionElement);
		}
		
		writeLocalExtensions(xmlDocument, localExtensionsPath);
	}
	
	
	public static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException { 
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setValidating(true);
		documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		
		return documentBuilderFactory;
	}
	
	
	public static DocumentBuilder newDocumentBuilder(final DocumentBuilderFactory documentBuilderFactory) throws ParserConfigurationException {
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		
		documentBuilder.setErrorHandler(new ErrorHandler() {
			@Override
		    public void fatalError(SAXParseException exception) throws SAXException {
				// Do nothing.
			}
	
		    @Override
		    public void error(SAXParseException exception) throws SAXException {
		    		// Do nothing.
		    }
	
		    @Override
		    public void warning(SAXParseException exception) throws SAXException {
		    		// Do nothing.
		    }
		    
		});
		
		return documentBuilder;
	}


	/**
	 * Write localextensions.xml out to disk.
	 * Note: The formatting sort of works, the new entries for <extension/> are indented
	 * but the overall indenting of the file is not consistent.  
	 */
	public static void writeLocalExtensions(Document xmlDocument, String localExtensionsPath) throws TransformerException {
		DOMSource domSource = new DOMSource(xmlDocument);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		StreamResult streamResult = new StreamResult(new File(localExtensionsPath));
        transformer.transform(domSource, streamResult);
	}

	
	/**
	 * Get the full path of a project in the workspace.
	 */
	public static IPath getProjectPath(final String projectName) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);

		if (project.exists()) {
			return project.getLocation();
		}
		
		return null;
	}
	
	
	/**
	 * Get the full path of the platform folder.
	 */
	public static IPath getPlatformPath() {
		return getProjectPath(PLATFORM);
	}

	
	/**
	 * Get the full path of the config folder.  
	 */
	public static IPath getConfigPath() {
		return getProjectPath(CONFIG);
	}

}
