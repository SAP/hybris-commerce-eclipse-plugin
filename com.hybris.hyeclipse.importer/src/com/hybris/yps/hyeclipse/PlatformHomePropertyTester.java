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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.Preferences;

import com.hybris.hyeclipse.commons.Constants;

public class PlatformHomePropertyTester extends PropertyTester {
	
	public PlatformHomePropertyTester() {}

	@Override
	public boolean test(Object arg0, String arg1, Object[] arg2, Object arg3) {
		
		boolean enableOption = false;
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		String platformHomeStr = preferences.get("platform_home", null);
		if (platformHomeStr == null) {
			IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
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
