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
package com.hybris.yps.hyeclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.wizards.Messages;

public class HyEclipsePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public HyEclipsePreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("SAP Commerce, development tools for Eclipse is a suite of Eclipse plugins for SAP Commerce projects that improves developer productivity.\n\nSee https://wiki.hybris.com/display/hyps/SAP+Hybris+Commerce+development+tools+for+Eclipse for details.");
	}
	
	@Override
	public void init(IWorkbench arg0) {}

	@Override
	protected void createFieldEditors() {
		final FileFieldEditor shell = new FileFieldEditor(HyEclipsePreferenceConstants.P_DEFAULT_SHELL,
				Messages.HyEclipsePreferencePage_shell_path, true, getFieldEditorParent());
		shell.setEmptyStringAllowed(false);
		addField(shell);
	}

}
