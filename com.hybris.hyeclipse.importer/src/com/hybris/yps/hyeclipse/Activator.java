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
package com.hybris.yps.hyeclipse;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.importer"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private com.hybris.hyeclipse.ytypesystem.Activator typeSystemExporter;
	

	/**
	 * The constructor
	 */
	public Activator() {
		super();
		if (plugin == null) {
			plugin = this; //NOSONAR			
		}
	}

	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		com.hybris.hyeclipse.commons.Activator.resetPlatformBootstrapBundle();
	}
	

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public com.hybris.hyeclipse.ytypesystem.Activator getTypeSystemExporter() {
		if (typeSystemExporter == null) {
			typeSystemExporter = com.hybris.hyeclipse.ytypesystem.Activator.getDefault();
		}
		return typeSystemExporter;
	}
	
	public void resetPlatform(String platformHome) {
		com.hybris.hyeclipse.commons.Activator.resetPlatformBootstrapBundle();
		
		try {
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			preferences.put("platform_home", platformHome);
			preferences.flush();
		}
		catch (BackingStoreException e) {
			logError("Failed to persist platform_home", e);
		}
		
		getTypeSystemExporter().setPlatformHome(null);
		getTypeSystemExporter().nullifySystemConfig();
		getTypeSystemExporter().nullifyPlatformConfig();
		getTypeSystemExporter().nullifyTypeSystem();
		getTypeSystemExporter().nullifyAllTypes();
		getTypeSystemExporter().nullifyAllTypeNames();
	}
	
	public Set<ExtensionHolder> getAllExtensionsForPlatform() {
		
		return getTypeSystemExporter().getAllExtensionsForPlatform();
	}

	public String getConfigDirectory() {
		
		return getTypeSystemExporter().getConfigDirectory();
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
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, msg, e);
		}
		else {
			status = new Status(IStatus.INFO, Activator.PLUGIN_ID, IStatus.OK, msg, e);
		}
		getLog().log(status);
	}
}
