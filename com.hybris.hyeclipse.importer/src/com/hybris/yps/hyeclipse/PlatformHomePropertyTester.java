package com.hybris.yps.hyeclipse;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

public class PlatformHomePropertyTester extends PropertyTester {
	
	public PlatformHomePropertyTester() {}

	@Override
	public boolean test(Object arg0, String arg1, Object[] arg2, Object arg3) {
		
		boolean enableOption = false;
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		String platformHomeStr = preferences.get("platform_home", null);
		if (platformHomeStr == null) {
			IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
			IPath platformProjectPath = platformProject.getLocation();
			if (platformProjectPath != null) {
				enableOption = true;
			}
		}
		else {
			enableOption = true;
		}
		
		return enableOption;
	}

}
