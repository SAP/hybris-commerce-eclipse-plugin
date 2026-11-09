/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.utils;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceConstants;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceInitializer;

public class ShellImporterPreferenceTest {

	private IPreferenceStore store;
	private String originalShell;
	private boolean originallyDefault;

	@Before
	public void savePreference() {
		store = Activator.getDefault().getPreferenceStore();
		originallyDefault = store.isDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		originalShell = store.getString(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
	}

	@After
	public void restorePreference() {
		if (originallyDefault) {
			store.setToDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		} else {
			store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, originalShell);
		}
	}

	@Test
	public void defaultIsInitializedWithoutOpeningPreferencePage() {
		store.setToDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(),
				store.getDefaultString(HyEclipsePreferenceConstants.P_DEFAULT_SHELL));
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(), new ShellImporter().defaultShell());
		if (!Platform.OS_WIN32.equals(Platform.getOS())) {
			Assert.assertEquals("/bin/zsh", new ShellImporter().defaultShell());
		}
	}

	@Test
	public void usesUserShellForAntCommand() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "/bin/zsh");
		final String shell = new ShellImporter().defaultShell();
		Assert.assertEquals("/bin/zsh", shell);
		Assert.assertEquals(shell, ShellImporter.antCommand(shell, false).get(0));
	}

	@Test
	public void trimsShellPathButPreservesEmbeddedSpaces() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "  /custom tools/bin/bash  ");
		Assert.assertEquals("/custom tools/bin/bash", new ShellImporter().defaultShell());
	}

	@Test
	public void blankShellFallsBackToPlatformDefault() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "  ");
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(), new ShellImporter().defaultShell());
	}
}
