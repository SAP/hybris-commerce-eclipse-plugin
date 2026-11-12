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
package com.hybris.yps.hyeclipse.preferences;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.hybris.yps.hyeclipse.Activator;

/** Initializes platform-appropriate defaults for the importer preference page. */
public class HyEclipsePreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		Activator.getDefault().getPreferenceStore().setDefault(HyEclipsePreferenceConstants.P_DEFAULT_SHELL,
				getDefaultShell());
		Activator.getDefault().getPreferenceStore().setDefault(HyEclipsePreferenceConstants.P_ANT_GOAL,
				HyEclipsePreferenceConstants.DEFAULT_ANT_GOAL);
		Activator.getDefault().getPreferenceStore().setDefault(HyEclipsePreferenceConstants.P_ANT_JDK,
				StringUtils.EMPTY);
	}

	/**
	 * @return the platform's default shell or Windows command processor
	 */
	public static String getDefaultShell() {
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			return getWindowsDefaultShell();
		}
		// On macOS and Unix, prefer the user's active $SHELL if valid, or /bin/zsh on macOS
	    final String userShell = System.getenv("SHELL"); //$NON-NLS-1$
	    if (StringUtils.isNotBlank(userShell) && new File(userShell.trim()).canExecute()) {
	        return userShell.trim();
	    }
	    if (Platform.OS_MACOSX.equals(Platform.getOS())) {
	        if (new File("/opt/homebrew/bin/bash").canExecute()) { //$NON-NLS-1$
	            return "/opt/homebrew/bin/bash"; //$NON-NLS-1$
	        }
	        return "/bin/zsh"; //$NON-NLS-1$
	    }
	    return HyEclipsePreferenceConstants.DEFAULT_SHELL;
	}

	/**
	 * @return the Windows command processor from ComSpec, or cmd.exe
	 */
	public static String getWindowsDefaultShell() {
		final String shell = System.getenv("ComSpec"); //$NON-NLS-1$
		return StringUtils.isNotBlank(shell) ? shell.trim() : HyEclipsePreferenceConstants.WINDOWS_DEFAULT_SHELL;
	}
}
