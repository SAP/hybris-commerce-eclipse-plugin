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
package com.hybris.hyeclipse.extgen.wizards;

import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.hybris.hyeclipse.extgen.Activator;
import com.hybris.hyeclipse.extgen.utils.ExtensionUtils;
import com.hybris.hyeclipse.extgen.utils.PathUtils;

/**
 * {@link WizardPage} for {@link NewExtensionWizard}
 */
public class NewExtensionWizardPage extends WizardPage {

	private Text extensionNameText;
	private Text extensionPackageText;
	private Combo extensionTemplateCombo;
	private Combo workingSetCombo;
	private DirectoryFieldEditor extensionDirectoryField;
	private Button autoImportCheckbox;
	private Button coreModuleCheckbox;
	private Button webModuleCheckbox;
	private static final String PLUGIN_ID = "com.hybris.hyeclipse.preferences";

	public NewExtensionWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("New Extension");
		setDescription("This wizard creates a new Hybris extension.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		extensionNameText = prepareTextGUI(container, "&Name:", 2);
		extensionPackageText = prepareTextGUI(container, "&Package:", 2);
		extensionTemplateCombo = prepareComboGUI(container, "&Template:", 2);
		extensionDirectoryField = prepareDirectoryFieldEditorGUI(container, " extension location: ");

		coreModuleCheckbox = prepareCheckboxGUI(container, "&Core module", 1, true);
		webModuleCheckbox = prepareCheckboxGUI(container, "&Web module", 2, false);
		autoImportCheckbox = prepareCheckboxGUI(container, "&Auto import", 3, true);
		workingSetCombo = prepareComboGUI(container, "&Working Set:", 2);
		initialize();
		dialogChanged();
		setControl(container);
	}

	/**
	 * Initializes GUI elements of {@link NewExtensionWizardPage}
	 */
	private void initialize() {
		ExtensionUtils.getTemplates().stream().forEach(template -> extensionTemplateCombo.add(template));
		extensionTemplateCombo.setText("yempty");
		IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getAllWorkingSets();
		if (workingSets != null)
		{
			for (IWorkingSet workingSet: workingSets)
			{
				if (!workingSet.getName().startsWith("Aggregate"))
				{
					workingSetCombo.add(workingSet.getName());
				}
			}
		}
		extensionDirectoryField.setStringValue(getDefaultExtensionLocation());
	}
	
	public String getDefaultExtensionLocation()
	{
		IEclipsePreferences prefs =  InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String location = prefs.get("last_save_location", PathUtils.getDefaultExtensionDirPath());
		return location;
	}

	public void setDefaultExtensionLocation(String location)
	{
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		prefs.put("last_save_location", location);
		 try {
		    // prefs are automatically flushed during a plugin's "super.stop()".
		    prefs.flush();  
		  } catch (BackingStoreException e) {
			  Activator.logError("Couldn't save default location for custom extensions", e);;
		}
	}
	/**
	 * Validates if parameters for extension are correct
	 */
	private void dialogChanged() {
		File saveLocation = getExtensionDirectory();
		String extensionName = getExtensionName();
		String packageName = getPackageName();
		String templateName = getTemplateName();
		Pattern namePattern = Pattern.compile("[^a-z0-9_-]", Pattern.CASE_INSENSITIVE);
		Pattern packagePattern = Pattern.compile("[^a-z0-9.]", Pattern.CASE_INSENSITIVE);
		if (!getAutoImportCheckboxSelection()) {
			coreModuleCheckbox.setSelection(false);
			webModuleCheckbox.setSelection(false);
			coreModuleCheckbox.setEnabled(false);
			webModuleCheckbox.setEnabled(false);
		} else {
			coreModuleCheckbox.setEnabled(true);
			webModuleCheckbox.setEnabled(true);
		}
		if (extensionName.length() == 0) {
			updateStatus("Extension name must be specified");
			return;
		}

		if (namePattern.matcher(extensionName).find()) {
			updateStatus("Extension name must be valid");
			return;
		}
		if (packageName.length() == 0) {
			updateStatus("Extension package must be specified");
			return;
		}
		if (packagePattern.matcher(packageName).find()) {
			updateStatus("Package name must be valid");
			return;
		}
		if (templateName.length() == 0 || !ExtensionUtils.getTemplates().contains(templateName)) {
			updateStatus("Template name must be specified correctly");
			return;
		}
		if (saveLocation.getAbsolutePath().length() == 0) {
			updateStatus("Save location must be specified");
			return;
		}
		if (!saveLocation.exists()) {
			updateStatus("Location must exist");
			return;
		}
		if (!saveLocation.isDirectory()) {
			updateStatus("Location must be a directory");
			return;
		}
		if (!saveLocation.isDirectory()) {
			updateStatus("Location must be a directory");
			return;
		}
		updateStatus(null);
	}

	/**
	 * Updates status with error message
	 * 
	 * @param message
	 *            message to be updated
	 */
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/**
	 * Creates {@link DirectoryFieldEditor} element for page container
	 * 
	 * @param container
	 *            container for which element is set
	 * @param label
	 *            label for the GUI element
	 * @return {@link DirectoryFieldEditor}
	 */
	private DirectoryFieldEditor prepareDirectoryFieldEditorGUI(Composite container, String label) {
		DirectoryFieldEditor field = new DirectoryFieldEditor("fileSelect", label, container);
		field.getTextControl(container).addModifyListener(e -> {
				setPageComplete(true);
				getWizard().getContainer().updateButtons();
				setErrorMessage(null);
		});
		return field;
	}

	/**
	 * Creates {@link Text} element for page container
	 * 
	 * @param container
	 *            container for which element is set
	 * @param label
	 *            label for the GUI element
	 * @param guiElement
	 *            {@link DirectoryFieldEditor} to be prepared
	 * @return {@link Text}
	 */
	private Text prepareTextGUI(Composite container, String labelText, int horizontalSpan) {
		Label label = new Label(container, SWT.NULL);
		label.setText(labelText);

		Text guiElement = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		guiElement.setLayoutData(gd);
		guiElement.addModifyListener(e -> dialogChanged());
		return guiElement;
	}

	/**
	 * Creates {@link Combo} element for page container
	 * 
	 * @param container
	 *            container for which element is set
	 * @param label
	 *            label for the GUI element
	 * @param guiElement
	 *            {@link Combo} to be prepared
	 * @return {@link Text}
	 */
	private Combo prepareComboGUI(Composite container, String labelText, int horizontalSpan) {
		Label label = new Label(container, SWT.READ_ONLY);
		label.setText(labelText);
		Combo guiElement = new Combo(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		guiElement.setLayoutData(gd);
		guiElement.addModifyListener( e -> dialogChanged());
		return guiElement;
	}

	/**
	 * Creates {@link Button} element for page container
	 * 
	 * @param container
	 *            container for which element is set
	 * @param label
	 *            label for the GUI element
	 * @return {@link Text}
	 */
	private Button prepareCheckboxGUI(Composite container, String labelText, int horizontalSpan, boolean checked) {
		Button guiElement = new Button(container, SWT.CHECK);
		guiElement.setText(labelText);
		guiElement.setSelection(checked);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		guiElement.setLayoutData(gd);
		guiElement.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				dialogChanged();
			}
		});
		return guiElement;
	}

	public File getExtensionDirectory() {
		if (extensionDirectoryField.getStringValue() != null) {
			String platformDirectoryFieldStr = extensionDirectoryField.getStringValue();
			platformDirectoryFieldStr = platformDirectoryFieldStr.trim();
			return new File(platformDirectoryFieldStr);
		} else {
			return null;
		}
	}

	public String getTemplateName() {
		return extensionTemplateCombo.getText();
	}

	public String getExtensionName() {
		return extensionNameText.getText();
	}

	public String getPackageName() {
		return extensionPackageText.getText();
	}

	public String getWorkingSet() {
		return workingSetCombo.getText();
	}

	public boolean getAutoImportCheckboxSelection() {
		return autoImportCheckbox.getSelection();
	}

	public boolean getCoreModuleCheckboxSelection() {
		return coreModuleCheckbox.getSelection();
	}

	public boolean getWebModuleCheckboxSelection() {
		return webModuleCheckbox.getSelection();
	}
}
