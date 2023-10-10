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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.osgi.service.prefs.Preferences;

import com.hybris.hyeclipse.commons.Constants;

public class CommandState extends AbstractSourceProvider {

	public final static String ID = "com.hybris.hyeclipse.properties.platformHome";
	public final static String ENABLED = "ENABLED";
	public final static String DISABLED = "DISABLED";
	
	@Override
	public void dispose() {
	}

	@Override
	public Map<String, String> getCurrentState() {
		Map<String, String> map = new HashMap<String, String>(1);
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
		
		if (enableOption) {
			map.put(ID, ENABLED);
		}
		else {
			map.put(ID, DISABLED);
		}
		return map;
	}
	
	public void setEnabled() {
        fireSourceChanged(ISources.WORKBENCH, ID, ENABLED);
    }

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { ID };
	}

}
