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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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
		final Group antSettings = new Group(getFieldEditorParent(), SWT.NONE);
		antSettings.setText(Messages.HyEclipsePreferencePage_ant_settings);
		antSettings.setLayout(new GridLayout(3, false));
		antSettings.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		final StringFieldEditor antGoal = new StringFieldEditor(HyEclipsePreferenceConstants.P_ANT_GOAL,
				Messages.HyEclipsePreferencePage_ant_goal, antSettings);
		antGoal.setEmptyStringAllowed(false);
		addField(antGoal);
		addField(new ComboFieldEditor(HyEclipsePreferenceConstants.P_ANT_JDK,
				Messages.HyEclipsePreferencePage_ant_jdk, AntJavaRuntime.getJdkChoices(), antSettings));
		final Label jdkInformation = new Label(antSettings, SWT.WRAP);
		jdkInformation.setText(Messages.HyEclipsePreferencePage_jdk_information);
		jdkInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		final Label separator = new Label(antSettings, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final Label shellInformation = new Label(antSettings, SWT.WRAP);
		shellInformation.setText(Messages.HyEclipsePreferencePage_shell_information);
		shellInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		final FileFieldEditor shell = new FileFieldEditor(HyEclipsePreferenceConstants.P_DEFAULT_SHELL,
				Messages.HyEclipsePreferencePage_shell_path, true, antSettings);
		shell.setEmptyStringAllowed(false);
		shell.setEnabled(!Platform.OS_WIN32.equals(Platform.getOS()), antSettings);
		addField(shell);
	}

}
