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
package com.hybris.hyeclipse.emf.beans.presentation.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BeansEditorPlugin extends AbstractUIPlugin {
	
	private static String PLUGIN_ID = "com.hybris.hyeclipse.emf.beans.editor";
	
	private static BeansEditorPlugin plugin;
	
	public BeansEditorPlugin() {
		super();
		if (plugin == null) {
			plugin = this; //NOSONAR			
		}
	}
	
	public static BeansEditorPlugin getDefault() {
		return plugin;
	}
	
	public static void logError(String message, Throwable ex) {
		BeansEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, ex));
	}

}
