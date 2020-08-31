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
package com.hybris.yps.hyeclipse.extensionmods;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import com.hybris.yps.hyeclipse.ExtensionHolder;

/**
 * This class implements an ICellModifier
 * An ICellModifier is called when the user modifies a cell in the 
 * tableViewer
 */
public class ExtensionCellModifier implements ICellModifier {
	
	private ModuleTableViewer moduleTableViewer;
	
	public ExtensionCellModifier(ModuleTableViewer moduleTableViewer) {
		super();
		this.moduleTableViewer = moduleTableViewer;
	}

	@Override
	public boolean canModify(Object element, String property) {
		ExtensionHolder extension = (ExtensionHolder)element;
		if (property.equalsIgnoreCase("name")) {
			return false;
		}
		if (property.equalsIgnoreCase("coreModule") && !(extension.isCoreModule())) {
			return false;
		}
		if (property.equalsIgnoreCase("webModule") && !(extension.isWebModule())) {
			return false;
		}
		if (property.equalsIgnoreCase("hmcModule") && !(extension.isHmcModule())) {
			return false;
		}
		return true;
	}

	@Override
	public Object getValue(Object element, String property) {
		// Find the index of the column
		int columnIndex = moduleTableViewer.getColumnNames().indexOf(property);
		Object result = null;
		ExtensionHolder extension = (ExtensionHolder)element;
		
		switch (columnIndex) {
			case 0 : // NAME_COLUMN 
				result = extension.getName();
				break;
			case 1 : // CORE_COLUMN 
				result = new Boolean(extension.isCoreModule());
				break;
			case 2 : // WEB_COLUMN 
				result = new Boolean(extension.isWebModule());					
				break;
			case 3 : // HMC_COLUMN 
				result = new Boolean(extension.isHmcModule());
				break;
			default :
				result = "";
		}
		return result;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		
		MessageBox dialog = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK| SWT.CANCEL);
		dialog.setText("Proceed with caution");
		dialog.setMessage("Disabling this module is not reversible, disabling will also clean  all assets (directories/files) for this module.");
		
		int returnCode = dialog.open();
		//Proceed only if OK
		if (returnCode == SWT.OK) {
			// Find the index of the column
			int columnIndex = moduleTableViewer.getColumnNames().indexOf(property);
			TableItem item = (TableItem) element;
			ExtensionHolder extension = (ExtensionHolder)item.getData();
			
			switch (columnIndex) {
				case 0 : // NAME_COLUMN
					//String valueString = ((String) value).trim();
					//extension.setName(valueString);
					break;
				case 1 : // CORE_COLUMN 
					extension.setCoreModule(((Boolean) value).booleanValue());
					break;
				case 2 : // WEB_COLUMN 
					extension.setWebModule(((Boolean) value).booleanValue());
					break;
				case 3 : // HMC_COLUMN
					extension.setHmcModule(((Boolean) value).booleanValue());
					break;
				default :
			}
			
			moduleTableViewer.getEmc().extensionChanged(extension);
		}
		
	}

}
