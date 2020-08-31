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
package com.hybris.impexformatter.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.editors.ColorProvider;

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
		
		PreferenceConverter.setDefault(store, PreferenceConstants.P_STRING_COLOR, ColorProvider.GREEN_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_COMMENT_COLOR, ColorProvider.GREY_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_INSTRUCT_COLOR, ColorProvider.DGREY_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_MODIF_COLOR, ColorProvider.PURPLE_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_TAG_COLOR, ColorProvider.PURPLE_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_KVAL_COLOR, ColorProvider.ORANGE_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_REF_COLOR, ColorProvider.RED_COLOR);
		PreferenceConverter.setDefault(store, PreferenceConstants.P_DEF_COLOR, ColorProvider.BLUE_COLOR);
		
	}

}
