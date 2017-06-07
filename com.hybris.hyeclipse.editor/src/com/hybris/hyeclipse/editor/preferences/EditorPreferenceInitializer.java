package com.hybris.hyeclipse.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.hybris.hyeclipse.editor.Activator;

public class EditorPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(EditorPreferenceConstants.COPYRIGHT_CONTENT,
				EditorPreferenceConstants.DEFAULT_COPYRIGHT_CONTENT);

		store.setDefault(EditorPreferenceConstants.COPYRIGHT_FIRST_LINE,
				EditorPreferenceConstants.DEFAULT_COPYRIGHT_FIRST_LINE);

		store.setDefault(EditorPreferenceConstants.COPYRIGHT_LINE_PREFIX,
				EditorPreferenceConstants.DEFAULT_COPYRIGHT_LINE_PREFIX);

		store.setDefault(EditorPreferenceConstants.COPYRIGHT_LAST_LINE,
				EditorPreferenceConstants.DEFAULT_COPYRIGHT_LAST_LINE);
	}

}
