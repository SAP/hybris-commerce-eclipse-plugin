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
