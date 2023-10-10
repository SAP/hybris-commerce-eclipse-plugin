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
package com.hybris.yps.hyeclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.Importer;

public class SynchronizePlatformWizard extends Wizard implements IImportWizard {

	private SynchronizePlatformPage page1;

	public SynchronizePlatformWizard() {
		this.page1 = new SynchronizePlatformPage();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(this.page1);
	}

	@Override
	public boolean canFinish() {
		return page1.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		if (page1.validatePage()) {
			synchronizePlatform();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Workhorse to do the actual import of the projects into this workspace.
	 */
	private void synchronizePlatform() {
		boolean autobuildEnabled = isAutoBuildEnabled();
		enableAutoBuild(false);
		final boolean fixClasspath = page1.isFixClasspath();
		final boolean removeHybrisBuilder = page1.isRemoveHybrisGenerator();
		final boolean createWorkingSets = page1.isCreateWorkingSets();
		final boolean useMultiThread = page1.isUseMultiThread();
		final boolean skipJarScanning = page1.isSkipJarScanning();

		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences"); //$NON-NLS-1$
		final String platformDir = preferences.get("platform_home", null); //$NON-NLS-1$

		if (platformDir == null) {
			throw new IllegalStateException(Messages.SynchronizePlatformWizard_missingPlatformDirectoryError);
		}

		IRunnableWithProgress importer = monitor -> 
				synchronizePlatform(monitor, new File(platformDir), fixClasspath, removeHybrisBuilder,
						createWorkingSets, useMultiThread, skipJarScanning);
		try {
			new ProgressMonitorDialog(getContainer().getShell()).run(true, false, importer);
		} catch (InvocationTargetException | InterruptedException e)
		{
			Activator.logError(Messages.SynchronizePlatformWizard_synchronizeError, e);
			MessageDialog.openError(this.page1.getControl().getShell(),
					Messages.SynchronizePlatformWizard_synchronizeError, e.getMessage()); // $NON-NLS-1$
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
		}
		enableAutoBuild(autobuildEnabled);
	}

	protected boolean isAutoBuildEnabled() {
		IPreferencesService service = Platform.getPreferencesService();
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		String key = Messages.SynchronizePlatformWizard_5;
		IScopeContext[] contexts = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE };
		return service.getBoolean(qualifier, key, false, contexts);
	}

	protected void enableAutoBuild(boolean enable) {
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(qualifier);
		node.putBoolean(Messages.SynchronizePlatformWizard_6, enable);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void synchronizePlatform(IProgressMonitor monitor, File platformDir,
			boolean fixClasspath, boolean removeHybrisGenerator, boolean createWorkingSets, boolean useMultiThread,
			boolean skipJarScanning) throws InvocationTargetException {
		try {
			new Importer().resetProjectsFromLocalExtensions(platformDir, monitor, fixClasspath, removeHybrisGenerator,
					createWorkingSets, useMultiThread, skipJarScanning);
		} catch (CoreException e) {
			Activator.logError("could not synchronize platform", e);
			throw new InvocationTargetException(e);
		} catch (InterruptedException e) {
			Activator.logError("could not synchronize platform", e);
			Thread.currentThread().interrupt();
		}
	}
}
