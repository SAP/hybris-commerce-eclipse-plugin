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
package com.hybris.yps.hyeclipse.jobs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;

import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.CommandState;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;
import com.hybris.yps.hyeclipse.utils.Importer;
import com.hybris.yps.hyeclipse.utils.ProjectJavaRuntime;
import com.hybris.yps.hyeclipse.utils.ProjectSourceJob;
import com.hybris.yps.hyeclipse.utils.ShellImporter;
import com.hybris.yps.hyeclipse.wizards.ImportJobData;
import com.hybris.yps.hyeclipse.wizards.Messages;

/** Imports a Commerce platform and runs follow-up work only after success. */
public class ImportJob extends Job {

	private final ImportJobData data;
	private final File sourceArchive;
	private SystemConfig systemConfig;
	private IVMInstall javaRuntime;
	private File javaHome;

	/**
	 * @param data selected platform and import options
	 */
	public ImportJob(final ImportJobData data) {
		this(data, null);
	}

	/**
	 * @param data selected platform and import options
	 * @param sourceArchive optional archive to attach after import
	 */
	public ImportJob(final ImportJobData data, final File sourceArchive) {
		super(Messages.ImportPlatformWizard_import_job_title);
		this.data = data;
		this.sourceArchive = sourceArchive;
	}

	@Override
	public IStatus run(final IProgressMonitor monitor) {
		try (final ProjectJavaRuntime.ChangeScope change = ProjectJavaRuntime.beginChange(monitor)) {
			return runImport(monitor);
		} catch (OperationCanceledException e) {
			monitor.done();
			return Status.CANCEL_STATUS;
		} catch (InterruptedException e) {
			monitor.done();
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		}
	}

	private IStatus runImport(final IProgressMonitor monitor) {
		final boolean autoBuild = ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		boolean interrupted = false;
		IStatus result = Status.OK_STATUS;
		try {
			setAutoBuild(false);
			checkCanceled(monitor);
			javaRuntime = AntJavaRuntime.getVMInstall();
			javaHome = AntJavaRuntime.getJavaHome(javaRuntime).getCanonicalFile();
			ProjectJavaRuntime.compilerCompliance(javaRuntime);
			// Do not remove existing projects if dependency preparation fails.
			final Collection<String> log = updateDependencies(monitor);
			if (!javaHome.equals(AntJavaRuntime.getJavaHome(javaRuntime).getCanonicalFile())) {
				throw new IOException("The selected JDK installation changed during Ant execution; retry the import");
			}
			this.systemConfig = SystemConfig.parse(log);
			this.systemConfig.validate();
			if (!data.platformDir().getCanonicalFile().equals(systemConfig.getPlatformHome())) {
				throw new IOException("Ant output belongs to a different platform");
			}
			this.systemConfig = systemConfig.withExtensionMetadata();
			checkCanceled(monitor);
			if (data.removeExistingProjects()) {
				removeExistingProjects(monitor);
			}
			importProjects(monitor);
			checkCanceled(monitor);
			// Persist before restoring auto-build: builders must see the imported configuration.
			saveSystemConfig();
		} catch (OperationCanceledException e) {
			result = Status.CANCEL_STATUS;
		} catch (InterruptedException e) {
			interrupted = true;
			result = Status.CANCEL_STATUS;
		} catch (CoreException | IOException | RuntimeException e) {
			Activator.logError(Messages.error_on_import, e);
			result = new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.error_on_import, e);
		} finally {
			// Workspace operations can consume interruption; restore it after cleanup.
			final boolean cleanupInterrupted = Thread.interrupted();
			try {
				setAutoBuild(autoBuild);
			} catch (CoreException e) {
				Activator.logError(Messages.ImportPlatformWizard_autobuild_error + autoBuild, e);
				result = new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, new IStatus[] { result },
						Messages.ImportPlatformWizard_autobuild_error + autoBuild, e);
			}
			monitor.done();
			if (interrupted || cleanupInterrupted) {
				Thread.currentThread().interrupt();
			}
		}
		if (result.isOK()) {
			importCompleted();
		}
		return result;
	}

	/**
	 * Returns the parsed system configuration from the Ant dependency update task,
	 * or {@code null} if dependencies have not been updated yet.
	 *
	 * @return the parsed {@link SystemConfig}
	 */
	public SystemConfig getSystemConfig() {
		return systemConfig;
	}

	/** @throws CoreException if the successful import cannot be persisted */
	protected void saveSystemConfig() throws CoreException {
		com.hybris.hyeclipse.commons.Activator.getDefault().getSystemConfigRepository().save(systemConfig);
		com.hybris.hyeclipse.commons.Activator.getDefault().getTypeSystemIndex().requestRefresh();
	}

	/**
	 * @param monitor cancellation monitor
	 * @return Ant output used to populate the system configuration
	 * @throws IOException if Ant fails
	 * @throws InterruptedException if interrupted
	 */
	protected Collection<String> updateDependencies(final IProgressMonitor monitor)
			throws IOException, InterruptedException {
		return new ShellImporter(javaRuntime).executeAntTask(data.platformDir(), monitor);
	}

	/**
	 * @param monitor import progress monitor
	 * @throws CoreException if workspace import fails
	 * @throws InterruptedException if interrupted
	 */
	protected void importProjects(final IProgressMonitor monitor) throws CoreException, InterruptedException {
		new Importer(this.systemConfig, javaRuntime).resetProjectsFromLocalExtensions(data.platformDir(), monitor,
				data.fixClasspath(), data.removeHybrisBuilder(), data.createWorkingSet(), data.useMultiThread(),
				data.skipJarScanning());
	}

	/** Enables commands on the display thread and attaches sources to imported projects. */
	protected void importCompleted() {
		if (sourceArchive != null) {
			new ProjectSourceJob(sourceArchive).schedule();
		}
		Display.getDefault().asyncExec(() -> {
			if (!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			final ISourceProviderService service = PlatformUI.getWorkbench().getService(ISourceProviderService.class);
			final CommandState state = (CommandState) service.getSourceProvider(CommandState.ID);
			if (state != null) {
				state.setEnabled();
			}
		});
	}

	private void removeExistingProjects(final IProgressMonitor monitor) throws CoreException {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		monitor.beginTask(Messages.ImportWizard_removing_extension, projects.length);
		for (final IProject project : projects) {
			checkCanceled(monitor);
			if (FixProjectsUtils.isAHybrisExtension(project)) {
				project.delete(false, true, monitor);
			}
			monitor.worked(1);
		}
	}

	private void setAutoBuild(final boolean enabled) throws CoreException {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(enabled);
		workspace.setDescription(description);
	}

	private void checkCanceled(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}
}
