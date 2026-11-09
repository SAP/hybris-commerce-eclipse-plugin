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
	private String originalGoal;
	private boolean goalOriginallyDefault;

	@Before
	public void savePreference() {
		store = Activator.getDefault().getPreferenceStore();
		originallyDefault = store.isDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		originalShell = store.getString(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		goalOriginallyDefault = store.isDefault(HyEclipsePreferenceConstants.P_ANT_GOAL);
		originalGoal = store.getString(HyEclipsePreferenceConstants.P_ANT_GOAL);
	}

	@After
	public void restorePreference() {
		if (originallyDefault) {
			store.setToDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		} else {
			store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, originalShell);
		}
		if (goalOriginallyDefault) {
			store.setToDefault(HyEclipsePreferenceConstants.P_ANT_GOAL);
		} else {
			store.setValue(HyEclipsePreferenceConstants.P_ANT_GOAL, originalGoal);
		}
	}

	@Test
	public void defaultIsInitializedWithoutOpeningPreferencePage() {
		store.setToDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(),
				store.getDefaultString(HyEclipsePreferenceConstants.P_DEFAULT_SHELL));
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(), new ShellImporter().defaultShell());
	}

	@Test
	public void usesUserShellForAntCommand() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "/bin/zsh");
		final ShellImporter importer = new ShellImporter() {
			@Override
			public boolean isWindows() {
				return false;
			}
		};
		final String shell = importer.defaultShell();
		Assert.assertEquals("/bin/zsh", shell);
		Assert.assertEquals(shell, importer.antCommand(shell, false).get(0));
	}

	@Test
	public void trimsShellPathButPreservesEmbeddedSpaces() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "  /custom tools/bin/bash  ");
		final ShellImporter importer = new ShellImporter() {
			@Override
			public boolean isWindows() {
				return false;
			}
		};
		Assert.assertEquals("/custom tools/bin/bash", importer.defaultShell());
	}

	@Test
	public void blankShellFallsBackToPlatformDefault() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "  ");
		Assert.assertEquals(HyEclipsePreferenceInitializer.getDefaultShell(), new ShellImporter().defaultShell());
	}

	@Test
	public void antGoalIsInitializedWithoutOpeningPreferencePage() {
		store.setToDefault(HyEclipsePreferenceConstants.P_ANT_GOAL);
		Assert.assertEquals(HyEclipsePreferenceConstants.DEFAULT_ANT_GOAL,
				store.getDefaultString(HyEclipsePreferenceConstants.P_ANT_GOAL));
		Assert.assertEquals(HyEclipsePreferenceConstants.DEFAULT_ANT_GOAL, new ShellImporter().antGoal());
	}

	@Test
	public void usesConfiguredGoalsOnUnixAndWindows() {
		store.setValue(HyEclipsePreferenceConstants.P_ANT_GOAL, "  clean all  ");
		final ShellImporter importer = new ShellImporter();
		Assert.assertEquals("clean all", importer.antGoal());
		Assert.assertTrue(importer.antCommand("/bin/zsh", false).get(3)
				.endsWith(" && java -version && exec ant clean all"));
		Assert.assertTrue(importer.antCommand("/bin/zsh", true).get(3)
				.endsWith(" && java -version && ant clean all"));
	}

	@Test
	public void blankAntGoalFallsBackToDefault() {
		store.setValue(HyEclipsePreferenceConstants.P_ANT_GOAL, "  ");
		Assert.assertEquals(HyEclipsePreferenceConstants.DEFAULT_ANT_GOAL, new ShellImporter().antGoal());
	}

	@Test
	public void windowsIgnoresConfiguredUnixShell() {
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "/bin/zsh");
		final ShellImporter importer = new ShellImporter() {
			@Override
			public boolean isWindows() {
				return true;
			}
		};
		Assert.assertEquals(HyEclipsePreferenceInitializer.getWindowsDefaultShell(), importer.defaultShell());
		Assert.assertEquals(HyEclipsePreferenceInitializer.getWindowsDefaultShell(),
				importer.antCommand("/bin/zsh", true).get(0));
	}
}
