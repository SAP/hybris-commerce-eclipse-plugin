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
package com.hybris.hyeclipse.hac.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.hac.Activator;
import com.hybris.hyeclipse.hac.Messages;

/**
 * Preference page for hAC
 */
public class HACPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/* Fields inputs */
	private StringFieldEditor hostname;
	private StringFieldEditor username;
	private StringFieldEditor password;
	private IntegerFieldEditor timeout;

	public HACPreferencePage() {
		super(GRID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(Messages.HACPP_SERVER_ENDPOINT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		// not allowing blank fields on Preference page
		if (Messages.HACPP_FIELD_EDITOR_VALUE.equals(event.getProperty())) {
			if (event.getSource() instanceof StringFieldEditor) {
				if (checkIfEmpty() && validateTimeout() && validateUrl()) {
					super.performApply();
					super.propertyChange(event);
					setValid(true);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		hostname = new StringFieldEditor(HACPreferenceConstants.P_HOSTNAME_URL, Messages.HACPP_HAC_URL,
				getFieldEditorParent());
		username = new StringFieldEditor(HACPreferenceConstants.P_USERNAME, Messages.HACPP_USERNAME,
				getFieldEditorParent());
		password = new StringFieldEditor(HACPreferenceConstants.P_PASSWORD, Messages.HACPP_PASSWORD,
				getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(final Composite parent, final int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		timeout = new IntegerFieldEditor(HACPreferenceConstants.P_TIMEOUT, Messages.HACPP_IMPORT_TIMEOUT,
				getFieldEditorParent());

		addField(hostname);
		addField(username);
		addField(password);
		addField(timeout);
	}

	/**
	 * Checks if editor fields are empty
	 */
	private boolean checkIfEmpty() {
		if (hostname.getStringValue().isEmpty() || username.getStringValue().isEmpty()
				|| password.getStringValue().isEmpty() || timeout.getStringValue().isEmpty()) {
			setValid(false);
			setErrorMessage(Messages.HACPP_ALL_FIELDS);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	/**
	 * Validate hac URL
	 * 
	 * @return true if hac URL is valid, false otherwise
	 */
	private boolean validateUrl() {
		final String hacUrl = hostname.getStringValue();
		String message = null;
		boolean isValid = true;
		
		// hacUrl cannot be empty and must start with either http:// or https://
		if( !HACPreferenceConstants.URL_PATTERN.matcher(hacUrl).matches() ) {
			isValid = false;
			message = Messages.HACPP_HAC_VALID_URL_MSG;
		}
		
		setValid(isValid);
		setErrorMessage(message);
		return isValid;
	}

	/**
	 * Checks if timeout field is positive number
	 */
	private boolean validateTimeout() {
		if( isInteger(timeout.getStringValue()) && timeout.getIntValue() > 0 ) {
			setErrorMessage(null);
			return true;
		} else {
			setErrorMessage(Messages.HACPP_TIMEOUT_INTEGER_MSG);
			return false;
		}
	}

	/**
	 * Checks if string represents an integer
	 *
	 * @param string
	 *            string to be checked
	 * @return true if string represents an integer
	 */
	private boolean isInteger(final String string) {
		try {
			Integer.parseInt(string);
		} catch (final NumberFormatException nfe) {
			return false;
		}
		return true;
	}
}
