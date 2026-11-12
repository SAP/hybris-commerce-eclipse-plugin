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
package com.hybris.yps.hyeclipse;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.jobs.UpdateWorkspaceJdkJob;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.preferences.JdkPreferenceChangeListener;
import com.hybris.yps.hyeclipse.utils.ProjectJavaRuntime;
import com.hybris.yps.hyeclipse.wizards.Messages;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.importer"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private com.hybris.hyeclipse.ytypesystem.Activator typeSystemExporter;
	private JdkPreferenceChangeListener jdkListener;
	private final AtomicLong jdkChangeVersion = new AtomicLong();
	private Job jdkPreparation;
	private UpdateWorkspaceJdkJob jdkUpdate;
	

	/**
	 * The constructor
	 */
	public Activator() {
		super();
		if (plugin == null) {
			plugin = this; //NOSONAR			
		}
	}

	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		com.hybris.hyeclipse.commons.Activator.resetPlatformBootstrapBundle();
		jdkListener = new JdkPreferenceChangeListener(getPreferenceStore(), this::requestJdkUpdatePrompt);
		getPreferenceStore().addPropertyChangeListener(jdkListener);
		JavaRuntime.addVMInstallChangedListener(jdkListener);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		jdkChangeVersion.incrementAndGet();
		if (jdkListener != null) {
			getPreferenceStore().removePropertyChangeListener(jdkListener);
			JavaRuntime.removeVMInstallChangedListener(jdkListener);
			jdkListener = null;
		}
		if (jdkPreparation != null) {
			jdkPreparation.cancel();
		}
		if (jdkUpdate != null) {
			jdkUpdate.cancel();
		}
		plugin = null;
		super.stop(context);
	}

	private void requestJdkUpdatePrompt() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		final long revision = jdkChangeVersion.incrementAndGet();
		final Display display = PlatformUI.getWorkbench().getDisplay();
		jdkPreparation = new Job(Messages.ProjectJavaRuntime_prepare_job) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					final IVMInstall vm = AntJavaRuntime.getVMInstall();
					final String compliance = ProjectJavaRuntime.compilerCompliance(vm);
					if (!monitor.isCanceled() && !display.isDisposed()) {
						display.asyncExec(() -> {
							if (!isCurrentJdkChange(revision)) {
								return;
							}
							final boolean confirmed = MessageDialog.openQuestion(display.getActiveShell(),
									Messages.ProjectJavaRuntime_update_title, MessageFormat.format(
											Messages.ProjectJavaRuntime_update_question, vm.getName(), compliance));
							if (confirmed && isCurrentJdkChange(revision)) {
								if (jdkUpdate != null) {
									jdkUpdate.cancel();
								}
								jdkUpdate = new UpdateWorkspaceJdkJob(vm);
								jdkUpdate.schedule();
							}
						});
					}
				} catch (IOException e) {
					if (!display.isDisposed()) {
						display.asyncExec(() -> {
							if (isCurrentJdkChange(revision)) {
								MessageDialog.openError(display.getActiveShell(),
										Messages.ProjectJavaRuntime_update_failed, e.getMessage());
							}
						});
					}
				}
				return Status.OK_STATUS;
			}
		};
		jdkPreparation.setSystem(true);
		jdkPreparation.schedule();
	}

	private boolean isCurrentJdkChange(final long revision) {
		return plugin == this && jdkListener != null && revision == jdkChangeVersion.get()
				&& PlatformUI.isWorkbenchRunning();
	}
	

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public com.hybris.hyeclipse.ytypesystem.Activator getTypeSystemExporter() {
		if (typeSystemExporter == null) {
			typeSystemExporter = com.hybris.hyeclipse.ytypesystem.Activator.getDefault();
		}
		return typeSystemExporter;
	}
	
	public void resetPlatform(String platformHome) {
		com.hybris.hyeclipse.commons.Activator.setPlatformHome(new File(platformHome));
		
		try {
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			preferences.put("platform_home", platformHome);
			preferences.flush();
		}
		catch (BackingStoreException e) {
			logError("Failed to persist platform_home", e);
		}
		
		getTypeSystemExporter().setPlatformHome(null);
	}
	
	public Set<ExtensionHolder> getAllExtensionsForPlatform() {
		return getTypeSystemExporter().getAllExtensionsForPlatform();
	}

	public String getConfigDirectory() {
		return getTypeSystemExporter().getConfigDirectory();
	}

	public static void log(String msg) {
		getDefault().log(msg, null);
	}
	
	public static void logError(String msg, Exception e) {
		getDefault().log(msg, e);
	}

	public void log(String msg, Exception e) {
		Status status = null;
		if (e != null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, msg, e);
		}
		else {
			status = new Status(IStatus.INFO, Activator.PLUGIN_ID, IStatus.OK, msg, e);
		}
		getLog().log(status);
	}
}
