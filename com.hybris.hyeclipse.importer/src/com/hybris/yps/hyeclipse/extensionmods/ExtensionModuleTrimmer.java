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
package com.hybris.yps.hyeclipse.extensionmods;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.HybrisUtil;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;

public class ExtensionModuleTrimmer {
	
	private ExtensionModuleTrimmer() {
	}
	
	public static void configureExtension(IProgressMonitor monitor, final ExtensionHolder extension) {
		// Do actual work here
		/*
		 * Web - removes the web directory Core - removes *-items.xml, src,
		 * gensrc, *-spring.xml HMC - removes hmc directory
		 */
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

				File extInfo = new File(extensionPath, Constants.EXTENSION_INFO_XML);
				Document doc = docBuilder.parse(extInfo);
				boolean updateProject = false;
				if (!extension.isCoreModule()) {
					String moduleName = "coremodule";
					removeAndAddComment(doc, moduleName);

					// Remove 'src' directory
					removeSourceFolder(monitor, project, "src", "src");
					// Remove 'gensrc' directory
					removeSourceFolder(monitor, project, "gensrc", "gensrc");

					// Remove *-items.xml, *-spring.xml
					project.getFile("resources/" + extension.getName() + "-spring.xml").delete(true, false, monitor);
					project.getFile("resources/" + extension.getName() + "-items.xml").delete(true, false, monitor);

					updateProject = true;
				}
				if (!extension.isWebModule()) {
					String moduleName = "webmodule";
					removeAndAddComment(doc, moduleName);

					// Remove 'web' directory
					removeSourceFolder(monitor, project, "web", "web/src");

					updateProject = true;
				}
				if (!extension.isHmcModule()) {
					String moduleName = "hmcmodule";
					removeAndAddComment(doc, moduleName);

					// Remove 'hmc' directory
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

					// Refresh local project
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			} catch (ParserConfigurationException | SAXException | IOException | CoreException | TransformerException e) {
				Activator.logError("InvocationTargetException", e);
			}

		}
		monitor.worked(5);
		monitor.done();
	}

	private static void removeSourceFolder(IProgressMonitor monitor, IProject project, String folderName,
			String classpathEntry) throws CoreException {
		IFolder webFolder = project.getFolder(folderName);
		if (webFolder != null) {
			webFolder.delete(true, false, monitor);
		}
		// Project is missing required source folder: 'web/src'
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject.isOnClasspath(project.getFolder(classpathEntry))) {
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
			for (int i = 0; i < attrs.getLength(); i++) {
				Attr attribute = (Attr) attrs.item(i);
				sb.append(" " + attribute.getName() + "=\"" + attribute.getValue() + "\"");
			}
			sb.append("/>");

			appendXmlFragment(extensionNode, sb.toString());
			extensionNode.removeChild(moduleNode);
		}
	}

	private static void appendXmlFragment(Node parent, String fragment) {
		Document doc = parent.getOwnerDocument();
		Comment comment = doc.createComment(fragment);
		parent.appendChild(comment);
	}

}
