package com.hybris.hyeclipse.script.executor.preferences;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.hybris.hyeclipse.commons.utils.PreferencesUtils;
import com.hybris.hyeclipse.script.executor.Activator;

/**
 * Initialize default values of a {@link HACScriptExecutionPreferencePage}
 * preferences.
 */
public class HACScriptExecutionPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * Initialize default values of a {@link HACScriptExecutionPreferencePage}
	 * preferences.
	 */
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		final Map<String, String> defaultScriptLanguages = new HashMap<>(
				HACScriptExecutionPreferenceConstants.DEFAULT_SCRIPT_LANGUAGES);

		PreferencesUtils.saveObjectToStoreAsDefault(store, HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES,
				(Serializable) defaultScriptLanguages);
	}
}
