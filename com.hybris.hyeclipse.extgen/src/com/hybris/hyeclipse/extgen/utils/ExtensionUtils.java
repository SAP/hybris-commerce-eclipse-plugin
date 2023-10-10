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
package com.hybris.hyeclipse.extgen.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.utils.XmlScannerUtils;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;
import com.hybris.yps.hyeclipse.extensionmods.ExtensionModuleTrimmer;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;
import com.hybris.yps.hyeclipse.utils.WorkingSetsUtils;

/**
 * Utility class around extensions handling.
 */
public class ExtensionUtils {
	private static final String EXT_NAME = "extension";
	private static final String TEMPLATE_KEY = "input.template";
	private static final String NAME_KEY = "input.name";
	private static final String PACKAGE_KEY = "input.package";
	private static final String CFG_NAME = "yplatform_extgen";
	
	private ExtensionUtils() {
		throw new IllegalStateException("utility class");
	}
	
	/**
	 * Moves extension directory to the given directory
	 * 
	 * @param destination
	 *            destination directory
	 * @param extensionName
	 *            name of extension
	 * @throws IOException
	 */
	public static void moveExtension(File destination, String extensionName) throws IOException {
		File srcFile = new File(PathUtils.getDefaultExtensionPath(extensionName));
		FileUtils.moveDirectoryToDirectory(srcFile, destination, true);
	}

	/**
	 * Imports extension to the Eclipse workspace
	 * 
	 * @param source
	 *            source directory
	 * @param extensionName
	 *            name of extension
	 * @throws CoreException
	 */
	public static void importExtension(IProgressMonitor monitor, File source, String extensionName) throws CoreException {
		IProjectDescription description;
		String path =  Paths.get(source.getAbsolutePath(), extensionName, Constants.DOT_PROJECT).toAbsolutePath().toString();
		description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(path));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null);
		// remove the builder
		FixProjectsUtils.removeBuildersFromProject(monitor, project);
	}

	/**
	 * Runs extgen ant target
	 * 
	 * @param monitor
	 *            {@link IProgressMonitor}
	 * @param extensionName
	 *            name of extension
	 * @param packageName
	 *            name of the extension package
	 * @param template
	 *            extension template
	 * @throws InvocationTargetException
	 */
	public static void runExtgen(IProgressMonitor monitor, String extensionName, String packageName, String template)
			throws InvocationTargetException {
		try {
			Map<String, String> properties = new HashMap<>();
			properties.put(TEMPLATE_KEY, template);
			properties.put(NAME_KEY, extensionName);
			properties.put(PACKAGE_KEY, packageName);

			ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchConfigurationType("org.eclipse.ant.AntLaunchConfigurationType");

			ILaunchConfigurationWorkingCopy config = null;
			config = type.newInstance(null, CFG_NAME);
			config.setAttribute("org.eclipse.ui.externaltools.ATTR_ANT_TARGETS", "extgen");
			config.setAttribute("org.eclipse.ui.externaltools.ATTR_CAPTURE_OUTPUT", true);
			config.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", "${workspace_loc:/platform/build.xml}");
			config.setAttribute("org.eclipse.ui.externaltools.ATTR_SHOW_CONSOLE", true);
			config.setAttribute("org.eclipse.ui.externaltools.ATTR_ANT_PROPERTIES", properties);
			config.setAttribute("org.eclipse.ant.ui.DEFAULT_VM_INSTALL", true);
			config.setAttribute("org.eclipse.jdt.launching.MAIN_TYPE",
					"org.eclipse.ant.internal.launching.remote.InternalAntRunner");
			config.setAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "platform");
			config.setAttribute("org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER",
					"org.eclipse.ant.ui.AntClasspathProvider");
			config.setAttribute("process_factory_id", "org.eclipse.ant.ui.remoteAntProcessFactory");
			config.setAttribute("org.eclipse.debug.core.ATTR_REFRESH_SCOPE", "${workspace}");
			ILaunchConfiguration launchCfg = config.doSave();
			ILaunch launch = launchCfg.launch(ILaunchManager.RUN_MODE, monitor);
			final boolean[] buildWasSuccessful = new boolean[] { true };
			for (IProcess proc : launch.getProcesses()) {
				proc.getStreamsProxy().getErrorStreamMonitor().addListener((String text, IStreamMonitor m) -> {
					if (text.contains("BUILD FAILED")) {
						buildWasSuccessful[0] = false;
					}
				});
			}
			if (!buildWasSuccessful[0]) {
				throw new CoreException(new Status(IStatus.ERROR, "com.hybris.hyeclipse.extgen", "Ant build failed"));
			}

		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}

	}

	/**
	 * Adds extension to the local extension
	 * 
	 * @param source
	 *            source directory
	 * @param extensionName
	 *            name of extension
	 * 
	 * @throws TransformerException
	 */
	public static void addToLocalExtension(File source, String workingSetName, String extensionName)
			throws SAXException, IOException, ParserConfigurationException, TransformerException {
		File xmlFile = new File(PathUtils.getLocalExtensionsPath());
		DocumentBuilder dBuilder = XmlScannerUtils.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();

		Node extensions = doc.getElementsByTagName("extensions").item(0);
		Attr dirAttr = doc.createAttribute("dir");
		dirAttr.setValue(source + File.separator + extensionName);
		Element extension = doc.createElement(EXT_NAME);
		extension.setAttributeNode(dirAttr);
		if (!workingSetName.isEmpty()) {
			if (getWorkingSets().contains(workingSetName)) {
				extensions.insertBefore(extension, getFirstExtensionNodeFromWorkingSet(extensions, workingSetName));
			} else {
				Comment workingSet = doc.createComment(workingSetName);
				Text newLine = doc.createTextNode(" \n");
				extensions.appendChild(workingSet);
				extensions.appendChild(newLine);
				extensions.appendChild(extension);
			}
		} else {
			extensions.insertBefore(extension, getFirstExtensionNode(extensions));
		}
		Transformer transformer = XmlScannerUtils.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource domSource = new DOMSource(doc);
		StreamResult result = new StreamResult(xmlFile);
		transformer.transform(domSource, result);
	}

	/**
	 * Returns first node after the comment node with working set name
	 * 
	 * @param extensionsNode
	 *            extension node
	 * @param workingSetName
	 *            name of working set
	 */
	private static Node getFirstExtensionNodeFromWorkingSet(Node extensionsNode, String workingSetName) {
		final String extensionTag = EXT_NAME;
		final String commentTag = "#comment";
		Node child;
		NodeList nodes = extensionsNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			child = nodes.item(i);
			if (commentTag.equals(child.getNodeName()) && workingSetName.equals(child.getNodeValue()))
				for (int j = i; j < nodes.getLength(); j++) {
					child = nodes.item(j);
					if (extensionTag.equals(child.getNodeName()))
						return child;
				}
		}
		return null;
	}

	/**
	 * Returns first extension node
	 * 
	 * @param extensionsNode
	 *            extension node
	 */
	private static Node getFirstExtensionNode(Node extensionsNode) {
		final String extensionTag = EXT_NAME;
		NodeList nodes = extensionsNode.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (extensionTag.equals(child.getNodeName()))
				return child;
		}
		return null;
	}

	/**
	 * Configures web and core modules
	 * 
	 * @param monitor
	 *            the progress monitor
	 * @param extension
	 *            extension to be configured
	 */
	public static void configureModules(IProgressMonitor monitor, ExtensionHolder extension) {
		ExtensionModuleTrimmer.configureExtension(monitor, extension);
	}

	/**
	 * Checks whether the {@link IProject} is a extension template
	 * 
	 * @param extensionProject
	 *            extension project to be checked
	 */
	private static boolean isTemplate(IProject extensionProject) {
		String path = extensionProject.getLocation().toOSString();
		File extInfo = new File(path, Constants.EXTENSION_INFO_XML);
		if (extInfo.exists()) {
			try {
				DocumentBuilder dBuilder = XmlScannerUtils.newDocumentBuilder();
				Document doc = dBuilder.parse(extInfo);
				Node metaNode = doc.getElementsByTagName("meta").item(0);
				if (metaNode != null) {
					Node key = metaNode.getAttributes().getNamedItem("key");
					Node value = metaNode.getAttributes().getNamedItem("value");
					return key.getNodeValue().equals("extgen-template-extension")
							&& value.getNodeValue().equals("true");
				}
			} catch (SAXException | IOException | ParserConfigurationException e) {
				Activator.logError("Failed to parse "+ Constants.EXTENSION_INFO_XML +" for project " + extensionProject.getName() , e);
			}

		}
		return false;
	}

	/**
	 * Returns templates defined in ext-template directory as well as custom
	 * templates present in the workspace
	 */
	public static List<String> getTemplates() {
		File templatesDir = new File(PathUtils.getExtensionsTemplatePath());
		List<String> templates = Arrays.stream(templatesDir.listFiles(File::isDirectory)).map(File::getName)
				.collect(Collectors.toList());
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<String> customTemplates = Arrays.stream(projects).filter(ExtensionUtils::isTemplate)
				.map(IProject::getName).collect(Collectors.toList());
		templates.addAll(customTemplates);
		return templates;
	}

	/**
	 * Returns working sets specified in localextensions.xml
	 */
	public static Set<String> getWorkingSets() {
		Set<String> workingSets = new HashSet<>();
		File localExtensions = new File(PathUtils.getLocalExtensionsPath());
		try {
			DocumentBuilder dBuilder = XmlScannerUtils.newDocumentBuilder();
			Document doc = dBuilder.parse(localExtensions);
			XPath xPath = XPathFactory.newInstance().newXPath();
			String path = "//comment()[following-sibling::*[1][self::extension]]";
			NodeList commentNodes = (NodeList) xPath.compile(path).evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < commentNodes.getLength(); i++) {
				Node node = commentNodes.item(i);
				String workingSet = node.getNodeValue().trim();
				workingSets.add(workingSet);
			}
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
			Activator.logError("Couldn't parse working sets file", e);
		}
		return workingSets;
	}

	/**
	 * Creates working sets in workspace and adds generated extension to one of
	 * them
	 * 
	 * @param monitor
	 *            progress monitor
	 */
	public static void addExtensionToWorkingSets(IProgressMonitor monitor) {
		WorkingSetsUtils.organizeWorkingSetsFromLocalExtensions(monitor);
	}
}
