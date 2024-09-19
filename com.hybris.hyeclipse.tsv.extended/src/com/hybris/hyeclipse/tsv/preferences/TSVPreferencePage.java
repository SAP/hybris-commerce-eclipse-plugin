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
package com.hybris.hyeclipse.tsv.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.tsv.Activator;

public class TSVPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public TSVPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Colour");
	}
	
	public void createFieldEditors() {
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_HIGH_COLOR, "High Priority: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_MEDIUM_COLOR, "Medium Priority: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_LOW_COLOR, "Low Priority: ", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}
	
}
