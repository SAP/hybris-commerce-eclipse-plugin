package com.hybris.yps.hyeclipse;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
	private File platformHome;
	
	/**
	 * The constructor
	 */
	public Activator() {}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
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
				}
			}
			else {
				platformHome = new File(platformHomeStr);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
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
	
	public Set<ExtensionHolder> getAllExtensionsForPlatform(String platformHome) {
		
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
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR, msg, e);
		}
		else {
			status = new Status(Status.INFO, Activator.PLUGIN_ID, Status.OK, msg, e);
		}
		getLog().log(status);
	}

}
