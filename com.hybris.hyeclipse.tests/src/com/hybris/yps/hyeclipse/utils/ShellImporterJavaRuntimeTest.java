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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceConstants;

public class ShellImporterJavaRuntimeTest {

	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	private final List<IVMInstall> installations = new ArrayList<>();
	private IPreferenceStore store;
	private String originalSelection;
	private boolean selectionWasDefault;
	private IVMInstall originalDefault;

	@Before
	public void saveSettings() {
		store = Activator.getDefault().getPreferenceStore();
		selectionWasDefault = store.isDefault(HyEclipsePreferenceConstants.P_ANT_JDK);
		originalSelection = store.getString(HyEclipsePreferenceConstants.P_ANT_JDK);
		originalDefault = JavaRuntime.getDefaultVMInstall();
	}

	@After
	public void restoreSettings() throws Exception {
		if (selectionWasDefault) {
			store.setToDefault(HyEclipsePreferenceConstants.P_ANT_JDK);
		} else {
			store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, originalSelection);
		}
		JavaRuntime.setDefaultVMInstall(originalDefault, new NullProgressMonitor(), false);
		for (final IVMInstall vm : installations) {
			vm.getVMInstallType().disposeVMInstall(vm.getId());
		}
	}

	@Test
	public void selectorListsRegisteredJdksButNotJres() throws Exception {
		final IVMInstall jdk = installation(true);
		final IVMInstall jre = installation(false);
		final List<String> ids = new ArrayList<>();
		for (final String[] choice : AntJavaRuntime.getJdkChoices()) {
			ids.add(choice[1]);
		}
		Assert.assertTrue(ids.contains(JavaRuntime.getCompositeIdFromVM(jdk)));
		Assert.assertFalse(ids.contains(JavaRuntime.getCompositeIdFromVM(jre)));
	}

	@Test
	public void explicitSelectionUsesStableEclipseVmIdentifier() throws Exception {
		final IVMInstall jdk = installation(true);
		select(jdk);
		Assert.assertEquals(jdk.getInstallLocation(), AntJavaRuntime.getJavaHome());
	}

	@Test
	public void defaultSelectionUsesWorkspaceJdk() throws Exception {
		final IVMInstall jdk = installation(true);
		JavaRuntime.setDefaultVMInstall(jdk, new NullProgressMonitor(), false);
		store.setToDefault(HyEclipsePreferenceConstants.P_ANT_JDK);
		Assert.assertEquals(jdk.getInstallLocation(), AntJavaRuntime.getJavaHome());
	}

	@Test(expected = IOException.class)
	public void removedJdkDoesNotSilentlyFallBack() throws Exception {
		final IVMInstall jdk = installation(true);
		select(jdk);
		jdk.getVMInstallType().disposeVMInstall(jdk.getId());
		final String selected = store.getString(HyEclipsePreferenceConstants.P_ANT_JDK);
		Assert.assertTrue(Arrays.stream(AntJavaRuntime.getJdkChoices()).anyMatch(choice -> selected.equals(choice[1])));
		AntJavaRuntime.getJavaHome();
	}

	@Test(expected = IOException.class)
	public void jreCannotBeUsedForAntImports() throws Exception {
		select(installation(false));
		AntJavaRuntime.getJavaHome();
	}

	@Test(timeout = 15000)
	public void selectedJdkSurvivesShellStartupAndCommerceEnvironmentChanges() throws Exception {
		Assume.assumeFalse(Platform.OS_WIN32.equals(Platform.getOS()));
		Assume.assumeTrue(new File("/bin/bash").canExecute());
		final IVMInstall jdk = installation(true);
		select(jdk);
		final File platform = temporary.newFolder("platform with spaces");
		final File oldJava = new File(platform, "old-java-bin");
		Files.createDirectories(oldJava.toPath());
		executable(new File(jdk.getInstallLocation(), "bin/java"), "#!/bin/sh\necho selected-java\n");
		executable(new File(oldJava, "java"), "#!/bin/sh\necho wrong-java-17\n");
		executable(new File(oldJava, "ant"), "#!/bin/sh\necho \"ant-java-home=$JAVA_HOME\"\njava\n");
		Files.writeString(new File(platform, "setantenv.sh").toPath(),
				"test \"$JAVA_HOME\" = \"$HYECLIPSE_ANT_JAVA_HOME\" || return 19\n"
						+ "export JAVA_HOME=/wrong-java-17\nexport PATH=\"$PWD/old-java-bin:$PATH\"\n",
				StandardCharsets.UTF_8);
		final ShellImporter importer = new ShellImporter() {
			@Override
			String defaultShell() {
				return "/bin/bash";
			}
		};
		final ProcessBuilder builder = importer.antProcess(platform);
		Assert.assertEquals(jdk.getInstallLocation().getAbsolutePath(),
				builder.environment().get(AntJavaRuntime.JAVA_HOME));
		// Mimic an Eclipse environment pointing at the wrong Java before login startup.
		builder.environment().put(AntJavaRuntime.JAVA_HOME, "/wrong-java-17");
		builder.environment().put(AntJavaRuntime.PATH, oldJava.getAbsolutePath() + File.pathSeparator
				+ builder.environment().get(AntJavaRuntime.PATH));
		final List<String> output = new ArrayList<>();
		AntProcessRunner.execute(builder, new NullProgressMonitor(), output::add, output::add);
		Assert.assertTrue(output.contains("ant-java-home=" + jdk.getInstallLocation().getAbsolutePath()));
		Assert.assertEquals(2, output.stream().filter("selected-java"::equals).count());
		Assert.assertFalse(output.contains("wrong-java-17"));
	}

	private IVMInstall installation(final boolean jdk) throws Exception {
		final IVMInstallType type = JavaRuntime.getVMInstallTypes()[0];
		final VMStandin definition = new VMStandin(type, UUID.randomUUID().toString());
		definition.setName("Ant fixture " + definition.getId());
		final File home = temporary.newFolder("jdk's home " + definition.getId());
		if (jdk) {
			final File bin = new File(home, "bin");
			Files.createDirectories(bin.toPath());
			Files.createFile(new File(bin, Platform.OS_WIN32.equals(Platform.getOS()) ? "javac.exe" : "javac").toPath());
		}
		definition.setInstallLocation(home);
		final IVMInstall vm = definition.convertToRealVM();
		installations.add(vm);
		return vm;
	}

	private void select(final IVMInstall vm) {
		store.setValue(HyEclipsePreferenceConstants.P_ANT_JDK, JavaRuntime.getCompositeIdFromVM(vm));
	}

	private void executable(final File file, final String contents) throws IOException {
		Files.writeString(file.toPath(), contents, StandardCharsets.UTF_8);
		Assert.assertTrue(file.setExecutable(true));
	}
}
