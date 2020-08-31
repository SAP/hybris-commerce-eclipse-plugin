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
