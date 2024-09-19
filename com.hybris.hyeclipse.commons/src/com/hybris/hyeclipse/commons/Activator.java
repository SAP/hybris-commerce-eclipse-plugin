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
package com.hybris.hyeclipse.commons;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.commons"; //$NON-NLS-1$
	// used to disable the nature lookup during importing
	private static final String ORG_ECLIPSE_EPP_MPC_NATURELOOKUP = "org.eclipse.epp.mpc.naturelookup";
	private static final String ORG_ECLIPSE_EPP_MPC_UI_PREFS = "org.eclipse.epp.mpc.ui";

	// The shared instance
	private static Activator plugin;
	
	private static File platformHome;
	private static boolean natureLookup = false;
	
	public Activator() {
		super();
		if (plugin == null) {
			plugin = this; //NOSONAR			
		}
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public static File resetPlatformBootstrapBundle() {
		String platformHomeStr = null;
		if (platformHome == null) {
			
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			platformHomeStr = preferences.get("platform_home", null);
			if (platformHomeStr == null) {
				IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
				IPath platformProjectPath = platformProject.getLocation();
				if (platformProjectPath != null) {
					platformHome = platformProjectPath.toFile();
				}
			}
			else {
				platformHome = new File(platformHomeStr);
			}
		}
		return platformHome;
	}
	
	/**
	 * In oxygen there is an nature solution lookup feature that causes a dialog to open for each imported project. 
	 * We disable it since it's not a very useful feature.
	 * @return 
	 * 
	 * @return
	 */
	public static boolean disableProjectNatureSolutionLookup()
	{
		IEclipsePreferences prefs =  InstanceScope.INSTANCE.getNode(ORG_ECLIPSE_EPP_MPC_UI_PREFS);
		natureLookup = prefs.getBoolean(ORG_ECLIPSE_EPP_MPC_NATURELOOKUP, true);
		prefs.putBoolean(ORG_ECLIPSE_EPP_MPC_NATURELOOKUP, false);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
		return natureLookup;
	}
	
	/**
	 * Restores latest value after import.
	 * @return restored value
	 */
	public static boolean restoreProjectNatureSolutionLookup()
	{
		IEclipsePreferences prefs =  InstanceScope.INSTANCE.getNode(ORG_ECLIPSE_EPP_MPC_UI_PREFS);
		prefs.putBoolean(ORG_ECLIPSE_EPP_MPC_NATURELOOKUP, natureLookup);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
		return natureLookup;
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
