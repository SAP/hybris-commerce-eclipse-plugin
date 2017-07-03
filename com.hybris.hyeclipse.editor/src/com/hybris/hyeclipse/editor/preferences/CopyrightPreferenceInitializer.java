package com.hybris.hyeclipse.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.hybris.hyeclipse.editor.Activator;

/**
 * Initializer of copyright preferences
 */
public class CopyrightPreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(CopyrightPreferenceConstants.COPYRIGHT_CONTENT,
				CopyrightPreferenceConstants.DEFAULT_COPYRIGHT_CONTENT);

		store.setDefault(CopyrightPreferenceConstants.COPYRIGHT_FIRST_LINE,
				CopyrightPreferenceConstants.DEFAULT_COPYRIGHT_FIRST_LINE);

		store.setDefault(CopyrightPreferenceConstants.COPYRIGHT_LINE_PREFIX,
				CopyrightPreferenceConstants.DEFAULT_COPYRIGHT_LINE_PREFIX);

		store.setDefault(CopyrightPreferenceConstants.COPYRIGHT_LAST_LINE,
				CopyrightPreferenceConstants.DEFAULT_COPYRIGHT_LAST_LINE);
	}

}
