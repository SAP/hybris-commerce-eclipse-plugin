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
package com.hybris.hyeclipse.platform.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Plugin extends AbstractUIPlugin {
	
	private static Plugin plugin;
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Plugin getDefault() {
		return plugin;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		
		Plugin.plugin = this;
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		Plugin.plugin = null;
		super.stop(context);
	}

}
