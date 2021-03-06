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
package com.hybris.hyeclipse.property.configuration.handlers;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;

import com.hybris.hyeclipse.property.configuration.managers.PropertyManager;

/**
 * Handler for save property command
 */
public class SavePropertyHandler extends AbstractPropertyHandler {

	private static final String DIALOG_TILE = "Upload platform config";
	private static final String DIALOG_MESSAGE = "Are you sure, you want to upload selected properties to the platform?";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void execute(final Map<String, String> properties) {
		if( MessageDialog.openConfirm(getActiveShell(), DIALOG_TILE, DIALOG_MESSAGE) ) {
			final PropertyManager manager = new PropertyManager();
			
			if(manager.checkHacHealth()) {
				properties.forEach(manager::saveProperty); 
			}
		}
	} 
}
