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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/** Detects committed JDK preference changes, independent of the confirmation UI. */
public final class JdkPreferenceChangeListener implements IPropertyChangeListener, IVMInstallChangedListener {
	private final IPreferenceStore store;
	private final Runnable changed;

	/**
	 * @param store workspace import preferences
	 * @param changed callback for a changed effective JDK selection
	 */
	public JdkPreferenceChangeListener(final IPreferenceStore store, final Runnable changed) {
		this.store = store;
		this.changed = changed;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (HyEclipsePreferenceConstants.P_ANT_JDK.equals(event.getProperty())
				&& !Objects.equals(event.getOldValue(), event.getNewValue())) {
			changed.run();
		}
	}

	@Override
	public void defaultVMInstallChanged(final IVMInstall previous, final IVMInstall current) {
		if (previous != null && current != null
				&& StringUtils.isBlank(store.getString(HyEclipsePreferenceConstants.P_ANT_JDK))
				&& !JavaRuntime.getCompositeIdFromVM(previous).equals(JavaRuntime.getCompositeIdFromVM(current))) {
			changed.run();
		}
	}

	@Override
	public void vmChanged(final org.eclipse.jdt.launching.PropertyChangeEvent event) {
		/* JDT itself maintains references when installations are edited or renamed. */
	}

	@Override
	public void vmAdded(final IVMInstall vm) {
		/* Adding an installation does not change the selected JDK. */
	}

	@Override
	public void vmRemoved(final IVMInstall vm) {
		/* Keep unavailable selections visible; never silently select a different JDK. */
	}
}
