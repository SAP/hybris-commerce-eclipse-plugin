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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.wizards.Messages;

/** Resolves Ant's JDK from Eclipse's registered VM installations. */
public final class AntJavaRuntime {

	public static final String JAVA_HOME = "JAVA_HOME"; //$NON-NLS-1$
	public static final String PATH = "PATH"; //$NON-NLS-1$
	public static final String ANT_JAVA_HOME = "HYECLIPSE_ANT_JAVA_HOME"; //$NON-NLS-1$
	public static final String BIN_DIRECTORY = "bin"; //$NON-NLS-1$

	private AntJavaRuntime() {
	}

	/**
	 * @return display labels and stable VM identifiers for the preference selector
	 */
	public static String[][] getJdkChoices() {
		final List<IVMInstall> installations = new ArrayList<>();
		for (final IVMInstallType type : JavaRuntime.getVMInstallTypes()) {
			for (final IVMInstall vm : type.getVMInstalls()) {
				if (isJdk(vm)) {
					installations.add(vm);
				}
			}
		}
		installations.sort(Comparator.comparing(IVMInstall::getName)
				.thenComparing(JavaRuntime::getCompositeIdFromVM));
		final List<String[]> choices = new ArrayList<>();
		choices.add(new String[] { Messages.HyEclipsePreferencePage_workspace_jdk, StringUtils.EMPTY });
		final String selected = Activator.getDefault().getPreferenceStore()
				.getString(HyEclipsePreferenceConstants.P_ANT_JDK);
		boolean selectedAvailable = StringUtils.isBlank(selected);
		for (final IVMInstall vm : installations) {
			final String id = JavaRuntime.getCompositeIdFromVM(vm);
			String version = StringUtils.EMPTY;
			if (vm instanceof IVMInstall2) {
				version = ((IVMInstall2) vm).getJavaVersion();
			}
			final String label = vm.getName() + (StringUtils.isNotBlank(version) ? " - " + version : StringUtils.EMPTY)
					+ " (" + vm.getInstallLocation().getAbsolutePath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			choices.add(new String[] { label, id });
			selectedAvailable |= id.equals(selected);
		}
		// Keep removed selections visible rather than silently switching the user's JDK.
		if (!selectedAvailable) {
			choices.add(new String[] { Messages.HyEclipsePreferencePage_unavailable_jdk, selected });
		}
		return choices.toArray(new String[0][]);
	}

	/**
	 * @return the configured JDK home, or the workspace default JDK home
	 * @throws IOException if the selected installation is missing or is not a JDK
	 */
	public static File getJavaHome() throws IOException {
		final String selected = Activator.getDefault().getPreferenceStore()
				.getString(HyEclipsePreferenceConstants.P_ANT_JDK);
		final IVMInstall vm = StringUtils.isBlank(selected) ? JavaRuntime.getDefaultVMInstall()
				: JavaRuntime.getVMFromCompositeId(selected);
		if (vm == null || !isJdk(vm)) {
			throw new IOException("The Ant import JDK is unavailable. Select a JDK in Ant import settings "
					+ "and configure its installation under Java > Installed JREs."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return vm.getInstallLocation().getAbsoluteFile();
	}

	private static boolean isJdk(final IVMInstall vm) {
		final File home = vm.getInstallLocation();
		final String compiler = Platform.OS_WIN32.equals(Platform.getOS()) ? "javac.exe" : "javac"; //$NON-NLS-1$ //$NON-NLS-2$
		return home != null && new File(new File(home, BIN_DIRECTORY), compiler).isFile();
	}
}
