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
package com.hybris.hyeclipse.ytypesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import de.hybris.bootstrap.config.ExtensionInfo;
import de.hybris.bootstrap.config.PlatformConfig;
import de.hybris.bootstrap.typesystem.YExtension;
import de.hybris.bootstrap.typesystem.YTypeSystem;
import de.hybris.bootstrap.typesystem.YTypeSystemLoader;
import de.hybris.bootstrap.typesystem.xml.HybrisTypeSystemParser;
import de.hybris.bootstrap.xml.ParseAbortException;

public class YTypeSystemBuilder {
	
	YTypeSystem typeSystem = null;
	
	public static YTypeSystem buildTypeSystem() {
		
		boolean buildMode = false;
		
		PlatformConfig platformConfig = Activator.getDefault().getPlatformConfig();
		List<ExtensionInfo> allExtensions = platformConfig.getExtensionInfosInBuildOrder();
		List<String> extensionNames = convertInfosToExtensionNames(allExtensions);
		
		// Add ybootstrap.jar to the classpath
		URL url = null;
		
		try {
			url = new URL("file://" + Activator.getDefault().getPlatformHome() + "/bootstrap/bin/ybootstrap.jar");
		}
		catch (MalformedURLException e) {
			Activator.logError("MalformedURLException", e);
		}
					
		// Use URLClassLoader to load extension info
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url });
		Thread.currentThread().setContextClassLoader(urlClassLoader);
					
		initClassloader(allExtensions);
		YTypeSystem typeSystem = loadViaClassLoader(extensionNames, buildMode);
					
		Thread.currentThread().setContextClassLoader(originalClassLoader);
					
		return typeSystem;
	}
	
	private static List<String> convertInfosToExtensionNames(List<ExtensionInfo> allExtensions) {
		List<String> names = new LinkedList<String>();
		for (ExtensionInfo info : allExtensions) {
			names.add(info.getName());
		}
		return names;
	}
	
	private static void initClassloader(List<ExtensionInfo> allExtensions) {
		for (ExtensionInfo extInfo : allExtensions) {
			processExtension(extInfo);
		}
	}

	private static void processExtension(ExtensionInfo extInfo) {
		try {
			File f = extInfo.getExtensionDirectory().getAbsoluteFile();
			// add resources directory to the classpath, required for finding
			// the items.xml files
			File resourceDirectory = new File(f, "resources");
			if (resourceDirectory.exists()) {
				addToClassPath(resourceDirectory);
			}
			// add extension jar to the classpath
			File extensionJar = new File(f, "bin/" + extInfo.getName() + "server.jar");
			if (extensionJar.exists()) {
				addToClassPath(extensionJar);
			}
			// add classes directory to the classpath
			File classesDir = new File(f, "classes");
			if (classesDir.exists()) {
				addToClassPath(classesDir);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
					"Invalid extension directory [" + extInfo.getExtensionDirectory().getAbsolutePath() + "]");
		}
	}
	
	public static void addToClassPath(File classpathEntry) {
		try {
			URI u = classpathEntry.toURI();
			URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
			Class<URLClassLoader> urlClass = URLClassLoader.class;
			Method method = urlClass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(urlClassLoader, new Object[] { u.toURL() });
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid classpath entry [" + classpathEntry + "]");
		}
	}

	private static YTypeSystem loadViaClassLoader(List<String> extensionNames, boolean buildMode) {
		YTypeSystemLoader loader = null;
		HybrisTypeSystemParser parser = null;
		try {
			loader = new YTypeSystemLoader(new ExtendedYTypeSystem(buildMode), false);
			parser = new HybrisTypeSystemParser(loader, buildMode);

			for (String extName : extensionNames) {
				InputStream is = getTypeSystemAsStream(extName);
				try {
					parser.parseExtensionSystem(extName, is);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							Activator.logError("IOException", e);
						}
					}
				}
				
				YExtension extension = loader.getSystem().getExtension(extName);
				
				if (extension != null) {
					InputStream is2 = getDeploymentsAsStream(extName);
					try {
						parser.parseExtensionDeployments(loader.getSystem().getExtension(extName), is2);
					} finally {
					
						if (is2 != null) {
							try {
								is2.close();
							} catch (IOException e) {
								Activator.logError("IOException", e);
							}
						}
					}
				}
			}
			loader.finish();
			// loader.validate();
			return loader.getSystem();
		}
		catch (ParseAbortException e) {
			throw new IllegalArgumentException("unexpected parse error : " + e.getMessage(), e);
		}
	}

	private static InputStream getTypeSystemAsStream(String extName) {
		URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		InputStream inputStream = null;
		URL myURL = urlClassLoader.findResource(extName + "-items.xml");
		if (myURL != null) {
			try {
				inputStream = myURL.openStream();
			}
			catch (IOException e) {
				Activator.logError("IOException", e);
			}
		}
		if (inputStream == null) {
			myURL = urlClassLoader.findResource(extName + ".items.xml");
			if (myURL != null) {
				try {
					inputStream = myURL.openStream();
				}
				catch (IOException e) {
					Activator.logError("IOException", e);
				}
			}
		}

		return inputStream;
	}

	private static InputStream getDeploymentsAsStream(String extName) {
		URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		InputStream inputStream = null;
		URL myURL = urlClassLoader.findResource(extName + "-advanced-deployment.xml");
		if (myURL != null) {
			try {
				inputStream = myURL.openStream();
			}
			catch (IOException e) {
				Activator.logError("IOException", e);
			}
		}
		if (inputStream == null) {
			myURL = urlClassLoader.findResource(extName + ".advanced-deployment.xml");
			if (myURL != null) {
				try {
					inputStream = myURL.openStream();
				}
				catch (IOException e) {
					Activator.logError("IOException", e);
				}
			}
		}

		return inputStream;
	}

}
