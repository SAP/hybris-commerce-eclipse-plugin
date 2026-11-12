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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class JdkPreferenceChangeListenerTest {
	private final List<IVMInstall> installations = new ArrayList<>();

	@After
	public void disposeFixtures() {
		for (final IVMInstall vm : installations) {
			vm.getVMInstallType().disposeVMInstall(vm.getId());
		}
	}

	@Test
	public void onlyNotifiesForAnActualSavedJdkPreferenceChange() {
		final PreferenceStore store = new PreferenceStore();
		store.setDefault(HyEclipsePreferenceConstants.P_ANT_JDK, "");
		final AtomicInteger calls = new AtomicInteger();
		store.addPropertyChangeListener(new JdkPreferenceChangeListener(store, calls::incrementAndGet));
		store.setValue(HyEclipsePreferenceConstants.P_DEFAULT_SHELL, "/bin/bash");
		Assert.assertEquals(0, calls.get());
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, "first");
		Assert.assertEquals(1, calls.get());
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, "first");
		Assert.assertEquals(1, calls.get());
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, "second");
		Assert.assertEquals(2, calls.get());
	}

	@Test
	public void defaultVmChangesOnlyMatterWhenThePreferenceUsesWorkspaceDefault() {
		final PreferenceStore store = new PreferenceStore();
		final AtomicInteger calls = new AtomicInteger();
		final JdkPreferenceChangeListener listener = new JdkPreferenceChangeListener(store, calls::incrementAndGet);
		final IVMInstall first = installation();
		final IVMInstall second = installation();
		listener.defaultVMInstallChanged(first, second);
		Assert.assertEquals(1, calls.get());
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, "explicit");
		listener.defaultVMInstallChanged(first, second);
		Assert.assertEquals(1, calls.get());
	}

	@Test
	public void initializationAndUnchangedDefaultVmsDoNotNotify() {
		final PreferenceStore store = new PreferenceStore();
		final AtomicInteger calls = new AtomicInteger();
		final JdkPreferenceChangeListener listener = new JdkPreferenceChangeListener(store, calls::incrementAndGet);
		final IVMInstall vm = installation();
		listener.defaultVMInstallChanged(null, vm);
		listener.defaultVMInstallChanged(vm, null);
		listener.defaultVMInstallChanged(vm, vm);
		Assert.assertEquals(0, calls.get());
	}

	private IVMInstall installation() {
		final VMStandin definition = new VMStandin(JavaRuntime.getVMInstallTypes()[0], UUID.randomUUID().toString());
		definition.setName("change-test-" + definition.getId());
		definition.setInstallLocation(new File(System.getProperty("java.home")));
		final IVMInstall vm = definition.convertToRealVM();
		installations.add(vm);
		return vm;
	}
}
