package com.hybris.hyeclipse.hac.preferences;

import java.util.regex.Pattern;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.hac.Activator;

/**
 * Preference page for hAC
 */
public class HACPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * HAC preference page properties
	 */
	private interface PreferencePage {
		final String DESCRIPTION = "Server Endpoint";
		
		public interface Label {
			static final String USERNAME_INPUT_TEXT = "Username: ";
			static final String PASSWORD_INPUT_TEXT = "Password: ";
			static final String TIMEOUT_INPUT_TEXT = "Import timeout [s]: ";
			static final String HOSTNAME_URL_INPUT_TEXT = "Hybris HAC URL: ";
		}
		
		public interface Validation {
			/* event constant */
			static final String EVENT_FIELD_EDITOR_VALUE = "field_editor_value";
			
			static final String ALL_FIELDS_ARE_MANDATORY_ERROR = "Provide all fields.";
			static final String TIMEOUT_MUST_BE_A_POSITIVE_INTEGER = "Timeout must be a positive integer number.";
			static final String URL_INVALID_BEGINING = "Hybris HAC url must be valid url or ip and starts with http:// or https://";
		}
	}
	
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
		setDescription(PreferencePage.DESCRIPTION);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		// not allowing blank fields on Preference page
		if (PreferencePage.Validation.EVENT_FIELD_EDITOR_VALUE.equals(event.getProperty())) {
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
		hostname = new StringFieldEditor(HACPreferenceConstants.P_HOSTNAME_URL, PreferencePage.Label.HOSTNAME_URL_INPUT_TEXT,
				getFieldEditorParent());
		username = new StringFieldEditor(HACPreferenceConstants.P_USERNAME, PreferencePage.Label.USERNAME_INPUT_TEXT,
				getFieldEditorParent());
		password = new StringFieldEditor(HACPreferenceConstants.P_PASSWORD, PreferencePage.Label.PASSWORD_INPUT_TEXT,
				getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(final Composite parent, final int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		timeout = new IntegerFieldEditor(HACPreferenceConstants.P_TIMEOUT, PreferencePage.Label.TIMEOUT_INPUT_TEXT,
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
			setErrorMessage(PreferencePage.Validation.ALL_FIELDS_ARE_MANDATORY_ERROR);
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
			message = PreferencePage.Validation.URL_INVALID_BEGINING;
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
			setErrorMessage(PreferencePage.Validation.TIMEOUT_MUST_BE_A_POSITIVE_INTEGER);
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
