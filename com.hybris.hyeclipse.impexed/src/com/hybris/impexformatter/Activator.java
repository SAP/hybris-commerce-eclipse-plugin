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
package com.hybris.impexformatter;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;

public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.impexed"; //$NON-NLS-1$
	
	// The shared instance
	private static Activator plugin;
	
	private File platformHome;
	private com.hybris.hyeclipse.ytypesystem.Activator typeSystemExporter;
	
	public final static String IMPEX_PARTITIONING = "__impex_partitioning";
	
	public Activator() {}
		
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		String platformHomeStr = null;
		if (platformHome == null) {
			
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			platformHomeStr = preferences.get("platform_home", null);
			if (platformHomeStr == null) {
				IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
				IPath platformProjectPath = platformProject.getLocation();
				if (platformProjectPath != null) {
					platformHome = platformProjectPath.toFile();
					platformHomeStr = platformHome.getAbsolutePath();
				}
			}
			else {
				platformHome = new File(platformHomeStr);
			}
		}
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	public static Activator getDefault() {
		return plugin;
	}
	
	public com.hybris.hyeclipse.ytypesystem.Activator getTypeSystemExporter() {
		if (typeSystemExporter == null) {
			typeSystemExporter = com.hybris.hyeclipse.ytypesystem.Activator.getDefault();
		}
		return typeSystemExporter;
	}
	
	public List<String> getAllTypeNames() {
		
		return getTypeSystemExporter().getAllTypeNames();
	}
	
	public String getTypeLoaderInfo(String typeName) {
		
		return getTypeSystemExporter().getTypeLoaderInfo(typeName);
	}
	
	public List<String> getAllAttributeNames(String typeName) {
		return getTypeSystemExporter().getAllAttributeNames(typeName);
	}
	
	public String getAttributeName(String typeName, String blah) {
		return getTypeSystemExporter().getAttributeName(typeName, blah);
	}
	
	public static void log(String msg) {
		getDefault().log(msg, null);
	}
	
	public static void logError(String msg, Exception e) {
		getDefault().log(msg, e);
	}

	public void log(String msg, Exception e) {
		Status status = null;
		if (e != null) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR, msg, e);
		}
		else {
			status = new Status(Status.INFO, Activator.PLUGIN_ID, Status.OK, msg, e);
		}
		getLog().log(status);
	}
	
}
