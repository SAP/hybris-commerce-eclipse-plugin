///*******************************************************************************
// * Copyright 2020 SAP
// * 
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License.  You may obtain a copy
// * of the License at
// * 
// *   http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// * License for the specific language governing permissions and limitations under
// * the License.
// ******************************************************************************/
//package com.hybris.hyeclipse.tsvextended.services.impl;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.List;
//
//import org.apache.log4j.Logger;
//
//import com.hybris.hyeclipse.tsvextended.utils.TSVExtendedYTypeSystem;
//import com.hybris.ps.tsv.extended.service.impl.PlatformServiceImpl;
//
//import de.hybris.bootstrap.typesystem.YExtension;
//import de.hybris.bootstrap.typesystem.YTypeSystem;
//import de.hybris.bootstrap.typesystem.YTypeSystemLoader;
//import de.hybris.bootstrap.typesystem.xml.HybrisTypeSystemParser;
//import de.hybris.bootstrap.xml.ParseAbortException;
//
//public class PlatformServiceImplHyeclipse extends PlatformServiceImpl {
//	
//	private static final Logger LOG = Logger.getLogger(PlatformServiceImplHyeclipse.class);
//
//	@Override
//	public YTypeSystem loadViaClassLoader(List<String> extensionNames, boolean buildMode) {
//		
//		YTypeSystemLoader loader = null;
//		HybrisTypeSystemParser parser = null;
//		try {
//			loader = new YTypeSystemLoader(new TSVExtendedYTypeSystem(buildMode), false);
//			parser = new HybrisTypeSystemParser(loader, buildMode);
//
//			for (String extName : extensionNames) {
//				parser.parseExtensionSystem(extName,
//						getTypeSystemAsStream(extName));
//				YExtension extension = loader.getSystem().getExtension(extName);
//				if (extension == null) {
//					LOG.debug("Skipping load of advanced-deployments for "
//							+ extName
//							+ " because can not find extension object (no items.xml exists)");
//				}
//				else {
//					parser.parseExtensionDeployments(loader.getSystem()
//							.getExtension(extName),
//							getDeploymentsAsStream(extName));
//				}
//			}
//			loader.finish();
//			
//			return loader.getSystem();
//		}
//		catch (ParseAbortException e) {
//			throw new IllegalArgumentException("unexpected parse error : "
//					+ e.getMessage(), e);
//		}
//	}
//	
//	private InputStream getTypeSystemAsStream(String extName) {
//		
//		URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
//		InputStream inputStream = null;
//		
//		URL myURL = urlClassLoader.findResource(extName + "-items.xml");
//		if (myURL != null) {
//			try {
//				inputStream = myURL.openStream();
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		if (inputStream == null) {
//			myURL = urlClassLoader.findResource(extName + ".items.xml");
//			if (myURL != null) {
//				try {
//					inputStream = myURL.openStream();
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}
//			}	
//		}
//
//		return inputStream;
//	}
//	
//	private InputStream getDeploymentsAsStream(String extName) {
//		
//		URLClassLoader urlClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
//		InputStream inputStream = null;
//		
//		URL myURL = urlClassLoader.findResource(extName + "-advanced-deployment.xml");
//		if (myURL != null) {
//			try {
//				inputStream = myURL.openStream();
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		if (inputStream == null) {
//			myURL = urlClassLoader.findResource(extName + ".advanced-deployment.xml");
//			if (myURL != null) {
//				try {
//					inputStream = myURL.openStream();
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		return inputStream;
//	}
// 	
//}
