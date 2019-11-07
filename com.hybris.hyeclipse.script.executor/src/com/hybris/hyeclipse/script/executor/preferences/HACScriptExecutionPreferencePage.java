package com.hybris.hyeclipse.script.executor.preferences;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.commons.utils.PreferencesUtils;
import com.hybris.hyeclipse.script.executor.Activator;
import com.hybris.hyeclipse.script.executor.dialog.ScriptLanguageDialog;

/**
 * Logic for the hAC script execution preference page.
 */
public class HACScriptExecutionPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	
	private List scriptLanguagesList;
	private Map<String, String> scriptLanguagesExtensionsMap;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench work) {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createContents(Composite parent) {
		final Composite entryTable = new Composite(parent, SWT.NULL);

		// Create a data that takes up the extra space in the dialog .
		final GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		final GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		// Add in a dummy label for spacing
		new Label(entryTable, SWT.NONE);

		scriptLanguagesList = new List(entryTable, SWT.BORDER);
		scriptLanguagesList.setLayoutData(new GridData(GridData.FILL_BOTH));

		final Composite buttonComposite = new Composite(entryTable, SWT.NULL);

		final GridLayout buttonsLayout = new GridLayout();
		buttonsLayout.numColumns = 3;
		buttonComposite.setLayout(buttonsLayout);

		final Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
		addButton.setText(Messages.HACScriptExecutionPreferencePage_Buttons_Add);

		final Button modifyButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
		modifyButton.setText(Messages.HACScriptExecutionPreferencePage_Buttons_Modify);

		final Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
		removeButton.setText(Messages.HACScriptExecutionPreferencePage_Buttons_Remove);

		/* buttons logic */
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				final ScriptLanguageDialog dialog = new ScriptLanguageDialog(getShell(), scriptLanguagesExtensionsMap);

				if (Window.OK == dialog.open()) {
					addNewScript(dialog.getScriptLanguageName(), dialog.getScriptLanguageFileExtension());
				}
			}
		});

		modifyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				for (String key : scriptLanguagesList.getSelection()) {
					final ScriptLanguageDialog dialog = new ScriptLanguageDialog(getShell(),
							scriptLanguagesExtensionsMap, key);

					if (Window.OK == dialog.open()) {
						modifyScript(dialog.getScriptLanguageName(), dialog.getScriptLanguageFileExtension());
					}
				}
			}
		});

		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				for (String key : scriptLanguagesList.getSelection()) {
					removeScript(key);
				}
			}
		});

		fetchScriptLanguagesForStore();
		updateScriptList();

		return entryTable;
	}

	/**
	 * Adds new script to the list and preferences.
	 * 
	 * @param name
	 *            name of a script language
	 * @param fileExtension
	 *            file extension of a script
	 */
	protected void addNewScript(final String name, final String fileExtension) {
		scriptLanguagesList.add(name);
		scriptLanguagesExtensionsMap.put(name, fileExtension);
	}

	/**
	 * Modifies existing script
	 * 
	 * @param name
	 *            name of a script language
	 * @param fileExtension
	 *            file extension of a script language
	 */
	protected void modifyScript(final String name, final String fileExtension) {
		scriptLanguagesExtensionsMap.replace(name, fileExtension);
	}

	/**
	 * Removes script language.
	 * 
	 * @param name
	 *            name of script language to remove
	 */
	protected void removeScript(final String name) {
		scriptLanguagesList.remove(name);
		scriptLanguagesExtensionsMap.remove(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void performApply() {
		saveScriptToStore();
		super.performApply();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void performDefaults() {
		scriptLanguagesExtensionsMap = new HashMap<>(HACScriptExecutionPreferenceConstants.DEFAULT_SCRIPT_LANGUAGES);
		updateScriptList();
		saveScriptToStore();
		super.performDefaults();
	}

	/**
	 * Updates script list
	 */
	protected void updateScriptList() {
		if (scriptLanguagesExtensionsMap == null) {
			scriptLanguagesExtensionsMap = Collections.emptyMap();
		}
		scriptLanguagesList.removeAll();
		scriptLanguagesExtensionsMap.forEach((key, value) -> scriptLanguagesList.add(key));
	}

	/**
	 * Gets script languages from store
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	protected void fetchScriptLanguagesForStore() {
		Object result;
			result = PreferencesUtils
			.readObjectFromStore(getPreferenceStore(), HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES).orElse(null);
		if (result instanceof Map) {
			this.scriptLanguagesExtensionsMap = (Map<String, String>) result;
		} else {
			this.scriptLanguagesExtensionsMap = Collections.emptyMap();
		}
	}

	/**
	 * Saves scripts to the store
	 */
	protected void saveScriptToStore() {
		PreferencesUtils.saveObjectToStore(getPreferenceStore(),
				HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES, (Serializable) scriptLanguagesExtensionsMap);
	}
}
