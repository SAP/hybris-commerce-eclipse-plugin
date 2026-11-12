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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;

public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.impexed"; //$NON-NLS-1$
	
	public static final String IMPEX_PARTITIONING = "__impex_partitioning";
	
	// The shared instance
	private static Activator plugin;

	public Activator() {
		super();
		if (plugin == null) {
			plugin = this; // NOSONAR
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
	
	/** @return current shared generated-model snapshot */
	public TypeSystemSnapshot getTypeSystemSnapshot() {
		return com.hybris.hyeclipse.commons.Activator.getDefault().getTypeSystemIndex().getSnapshot();
	}
	
	public List<String> getAllTypeNames() {
		
		return getTypeSystemSnapshot().getAllTypeNames();
	}
	
	public List<String> getAllAttributeNames(String typeName) {
		return getTypeSystemSnapshot().getAllAttributeNames(typeName);
	}
	
	public String getAttributeName(String typeName, String qualifier) {
		final YAttributeDescriptor attribute = getTypeSystemSnapshot().getAttribute(typeName, qualifier);
		return attribute != null ? attribute.getQualifier() : null;
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
