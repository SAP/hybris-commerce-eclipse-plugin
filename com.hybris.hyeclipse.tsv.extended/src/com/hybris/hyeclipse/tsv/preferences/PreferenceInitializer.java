package com.hybris.hyeclipse.tsv.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.hybris.hyeclipse.tsv.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		Display display = Display.getCurrent();
		Color highColour = display.getSystemColor(SWT.COLOR_RED);
		Color mediumColour = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		Color lowColour = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		
		PreferenceConverter.setDefault(store, PreferenceConstants.P_PRIORITY_HIGH_COLOR, highColour.getRGB());
		PreferenceConverter.setDefault(store, PreferenceConstants.P_PRIORITY_MEDIUM_COLOR, mediumColour.getRGB());
		PreferenceConverter.setDefault(store, PreferenceConstants.P_PRIORITY_LOW_COLOR, lowColour.getRGB());
	}

}
