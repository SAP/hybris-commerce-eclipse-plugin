package com.hybris.hyeclipse.extgen.wizards;

import java.io.File;

import java.util.regex.Pattern;
import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

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

		extensionNameText = prepareTextGUI(container, "&Name:", 2, extensionNameText);
		extensionPackageText = prepareTextGUI(container, "&Package:", 2, extensionPackageText);
		extensionTemplateCombo = prepareComboGUI(container, "&Template:", 2, extensionTemplateCombo);
		extensionDirectoryField = prepareDirectoryFieldEditorGUI(container, "[y] extension location: ",
				extensionDirectoryField);

		coreModuleCheckbox = prepareCheckboxGUI(container, "&Core module", 1, coreModuleCheckbox, true);
		webModuleCheckbox = prepareCheckboxGUI(container, "&Web module", 2, webModuleCheckbox, false);
		autoImportCheckbox = prepareCheckboxGUI(container, "&Auto import", 3, autoImportCheckbox, true);
		workingSetCombo = prepareComboGUI(container, "&Working Set:", 2, workingSetCombo);
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
	 * @param extensionDirectoryField
	 *            {@link DirectoryFieldEditor} to be prepared
	 * @return {@link DirectoryFieldEditor}
	 */
	private DirectoryFieldEditor prepareDirectoryFieldEditorGUI(Composite container, String label,
			DirectoryFieldEditor extensionDirectoryField) {
		extensionDirectoryField = new DirectoryFieldEditor("fileSelect", label, container);
		extensionDirectoryField.getTextControl(container).addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setPageComplete(true);
				getWizard().getContainer().updateButtons();
				setErrorMessage(null);
			}
		});
		return extensionDirectoryField;
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
	private Text prepareTextGUI(Composite container, String labelText, int horizontalSpan, Text guiElement) {
		Label label = new Label(container, SWT.NULL);
		label.setText(labelText);

		guiElement = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		guiElement.setLayoutData(gd);
		guiElement.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
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
	private Combo prepareComboGUI(Composite container, String labelText, int horizontalSpan, Combo guiElement) {
		Label label = new Label(container, SWT.READ_ONLY);
		label.setText(labelText);
		guiElement = new Combo(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = horizontalSpan;
		guiElement.setLayoutData(gd);
		guiElement.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		return guiElement;
	}

	/**
	 * Creates {@link Button} element for page container
	 * 
	 * @param container
	 *            container for which element is set
	 * @param label
	 *            label for the GUI element
	 * @param guiElement
	 *            {@link Button} to be prepared
	 * @return {@link Text}
	 */
	private Button prepareCheckboxGUI(Composite container, String labelText, int horizontalSpan, Button guiElement,
			boolean checked) {
		guiElement = new Button(container, SWT.CHECK);
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