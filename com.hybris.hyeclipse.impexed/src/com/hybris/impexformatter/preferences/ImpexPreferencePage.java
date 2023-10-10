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
package com.hybris.impexformatter.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.impexformatter.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class ImpexPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public ImpexPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Colours");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		
		addField(new ColorFieldEditor(PreferenceConstants.P_COMMENT_COLOR, "Comments: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_DEF_COLOR, "Variables ($productCatalog, $languages,...): ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_TAG_COLOR, "Tags (INSERT, INSERT_UPDATE,...: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_INSTRUCT_COLOR, "Instructions: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_MODIF_COLOR, "Modifiers (unique, default,...: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_KVAL_COLOR, "Keywords (true, false,...: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_REF_COLOR, "References: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_STRING_COLOR, "Main Text: ", getFieldEditorParent()));
			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		
	}
	
}
