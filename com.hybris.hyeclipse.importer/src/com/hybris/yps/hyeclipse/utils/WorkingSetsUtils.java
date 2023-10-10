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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.yps.hyeclipse.Activator;

public class WorkingSetsUtils {
	public static boolean organizeWorkingSetsFromLocalExtensions(IProgressMonitor monitor) {
		boolean commentsFound = false;
		try {
			monitor.beginTask("Creating Working Sets", 10);
			// Parse localextensions.xml
			String configDirectory = FixProjectsUtils.getConfigDirectory();
			File localExtensionsFile = new File(configDirectory, "localextensions.xml");
			HashMap<String, List<String>> workingSetMap = new HashMap<String, List<String>>();

			try {
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				docFactory.setValidating(true);
				// prevent external entities
				docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				docBuilder.setErrorHandler(new ErrorHandler() {
					@Override
					public void fatalError(SAXParseException exception) throws SAXException {
					}

					@Override
					public void error(SAXParseException exception) throws SAXException {
					}

					@Override
					public void warning(SAXParseException exception) throws SAXException {
					}
				});
				Document localExtDocument = docBuilder.parse(localExtensionsFile);
				XPath xPath = XPathFactory.newInstance().newXPath();
				String path = "//comment()[following-sibling::*[1][self::extension]]";

				// NodeList of all the comments that are followed by extensions
				NodeList commentNodes = (NodeList) xPath.compile(path).evaluate(localExtDocument,
						XPathConstants.NODESET);
				monitor.worked(2);
				String workingSetKey = "";
				List<String> extensionList = new ArrayList<String>();

				for (int i = 0; i < commentNodes.getLength(); i++) {
					if (!workingSetKey.isEmpty() && !extensionList.isEmpty()) {
						// Already got a working set, put it in Map
						List<String> listForKey = workingSetMap.get(workingSetKey);
						if (listForKey != null) {
							extensionList.addAll(listForKey);
						}
						workingSetMap.put(workingSetKey, extensionList);
						workingSetKey = "";
						extensionList = new ArrayList<String>();
						listForKey = new ArrayList<String>();
					}
					// The comment node
					Node node = commentNodes.item(i);
					workingSetKey = node.getNodeValue().trim();

					boolean isComment = false;
					while (!isComment && node != null && workingSetKey.indexOf("extension") == -1) {
						Node extension = node.getNextSibling();
						if (extension != null) {
							if (extension.getNodeType() == Node.COMMENT_NODE
									&& extension.getNodeValue().indexOf("extension") == -1) {
								isComment = true;
							} else if (extension.getNodeType() == Node.ELEMENT_NODE) {
								String extensionName = null;
								NamedNodeMap nnm = extension.getAttributes();
								Node nameAttribute = nnm.getNamedItem("name");
								if (nameAttribute != null) {
									extensionName = nameAttribute.getNodeValue().trim();
									if (extensionName.isEmpty() == false) {
										extensionList.add(extensionName);
									}
								} else {
									nameAttribute = nnm.getNamedItem("dir");
									if (nameAttribute != null) {
										String fullPath = nameAttribute.getNodeValue().trim();
										extensionName = fullPath.substring(fullPath.lastIndexOf("/") + 1,
												fullPath.length());
										if (extensionName.isEmpty() == false) {
											extensionList.add(extensionName);
										}
									}
								}
							}
						}

						node = extension;
					}
				}
				if (!workingSetKey.isEmpty() && !extensionList.isEmpty()) {
					// Already got a working set, put it in Map
					List<String> listForKey = workingSetMap.get(workingSetKey);
					if (listForKey != null) {
						extensionList.addAll(listForKey);
					}
					workingSetMap.put(workingSetKey, extensionList);
				}

			} catch (SAXException e) {
				Activator.logError("SAXException", e);
			} catch (IOException e) {
				Activator.logError("IOException", e);
			} catch (ParserConfigurationException e) {
				Activator.logError("ParserConfigurationException", e);
			} catch (XPathExpressionException e) {
				Activator.logError("XPathExpressionException", e);
			}

			monitor.worked(2);
			// Create Working Set per comment
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkingSetManager manager = workbench.getWorkingSetManager();
			if (!workingSetMap.isEmpty()) {
				commentsFound = true;
				Set<String> keys = workingSetMap.keySet();
				for (String key : keys) {

					// Add projects to Working Set
					List<String> projectNames = workingSetMap.get(key);
					ArrayList<IProject> projectList = new ArrayList<IProject>();

					for (String projectName : projectNames) {
						IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
						IProject project = workspace.getProject(projectName);
						projectList.add(project);
					}

					IProject[] projectArray = projectList.toArray(new IProject[projectList.size()]);

					IWorkingSet existingWorkingSet = manager.getWorkingSet(key);
					if (existingWorkingSet == null) {
						IWorkingSet workingSet = manager.createWorkingSet(key, projectArray);
						manager.addWorkingSet(workingSet);
					} else {
						// Already got a Working Set with this name
						existingWorkingSet.setElements(projectArray);
					}
				}
			}
			monitor.worked(6);
		} finally {
			monitor.done();
		}
		return commentsFound;
	}
	
	public static void organizeWorkingSetsFromExtensionDirectories(IProgressMonitor monitor) {

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkingSetManager manager = workbench.getWorkingSetManager();

		Map<String, Set<IProject>> extToProjectMap = new HashMap<String, Set<IProject>>();

		IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		monitor.beginTask("Creating Working Sets", allProjects.length);
		int projectsProcessed = 0;
		for (IProject project : allProjects) {
			// projects to ignore
			if (project.getName().equals("RemoteSystemsTempFiles") || project.getName().equals(Constants.PLATFROM) || project.getName().equals("config"))
			{ 
				continue;
			}
			File directory = project.getLocation().toFile().getParentFile();
			String directoryName = directory.getName();
			if (!extToProjectMap.containsKey(directoryName)) {
				extToProjectMap.put(directoryName, new HashSet<IProject>());
				extToProjectMap.get(directoryName).add(ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM));
				extToProjectMap.get(directoryName).add(ResourcesPlugin.getWorkspace().getRoot().getProject("config"));
			}
			extToProjectMap.get(directoryName).add(project);
			projectsProcessed++;
			monitor.worked(projectsProcessed);
		}
		monitor.done();

		// now create the working sets
		for (Entry<String, Set<IProject>> entry : extToProjectMap.entrySet()) {
			IWorkingSet existingWorkingSet = manager.getWorkingSet(entry.getKey());
			IProject[] projectsArray = entry.getValue().toArray(new IProject[entry.getValue().size()]);
			if (existingWorkingSet == null) {
				IWorkingSet workingSet = manager.createWorkingSet(entry.getKey(), projectsArray);
				manager.addWorkingSet(workingSet);
			} else {
				// Already got a Working Set with this name
				existingWorkingSet.setElements(projectsArray);
			}
		}
	}
		
}
