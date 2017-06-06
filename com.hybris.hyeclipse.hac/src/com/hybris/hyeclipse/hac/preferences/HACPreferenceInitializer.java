package com.hybris.hyeclipse.hac.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.hybris.hyeclipse.hac.Activator;

/**
 * Class used to initialize default preference values.
 */
public class HACPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(HACPreferenceConstants.P_HOSTNAME_URL,
				HACPreferenceConstants.V_HOSTNAME_URL);
		store.setDefault(HACPreferenceConstants.P_USERNAME, HACPreferenceConstants.V_USERNAME);
		store.setDefault(HACPreferenceConstants.P_PASSWORD, HACPreferenceConstants.V_PASSWORD);
		store.setDefault(HACPreferenceConstants.P_TIMEOUT, HACPreferenceConstants.V_TIMEOUT);
	}
}
