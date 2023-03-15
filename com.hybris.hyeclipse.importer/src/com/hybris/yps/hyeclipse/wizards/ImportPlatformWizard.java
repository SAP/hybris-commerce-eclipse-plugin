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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.CommandState;
import com.hybris.yps.hyeclipse.utils.AntImporter;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;
import com.hybris.yps.hyeclipse.utils.Importer;
import com.hybris.yps.hyeclipse.utils.ProjectSourceJob;
import com.hybris.yps.hyeclipse.utils.ShellImporter;

/**
 * Wizard to walk the user through importing all projects from a given platform
 * directory.
 * 
 * @author brendan
 * @author Pawel Wolanski
 *
 */
public class ImportPlatformWizard extends Wizard implements IImportWizard {
	private ImportPlatformPage page1;
	private AttachSourcesPage page2;

	class ImportJob extends Job {

		private boolean removeExistingProjects;
		private boolean fixClasspath;
		private boolean removeHybrisBuilder;
		private boolean createWorkingSet;
		private boolean useMultiThread;
		private boolean skipJarScanning;
		private File platformDir;

		public ImportJob() {
			super(Messages.ImportPlatformWizard_import_job_title);
			removeExistingProjects = page1.isRemoveExistingProjects();
			fixClasspath = page1.isFixClasspath();
			removeHybrisBuilder = page1.isRemoveHybrisGenerator();
			createWorkingSet = page1.isCreateWorkingSets();
			useMultiThread = page1.isUseMultiThread();
			skipJarScanning = page1.isSkipJarScanning();
			platformDir = page1.getPlatformDirectory();
		}

		public IStatus run(IProgressMonitor monitor) {
			enableAutoBuild(false);

			List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
			if (removeExistingProjects && projects != null && (!projects.isEmpty())) {
				monitor.setTaskName(Messages.ImportWizard_removing_extension);
				monitor.beginTask(Messages.ImportWizard_removing_extension, projects.size()); // $NON-NLS-1$
				int step = 0;
				for (IProject project : projects) {
					try {
						if (FixProjectsUtils.isAHybrisExtension(project)) {
							project.delete(false, true, monitor);
						}
					} catch (CoreException e) {
						ErrorDialog.openError(page1.getControl().getShell(), Messages.error_on_import,
								Messages.error_on_import_info, createErrorStatus(e));
						Thread.currentThread().interrupt();
					}
					step++;
					monitor.worked(step);
				}
			}
			try {
				importPlatform(monitor, platformDir, fixClasspath, removeHybrisBuilder, createWorkingSet,
						useMultiThread, skipJarScanning);
			} catch (InvocationTargetException | InterruptedException e) {
				Activator.logError(Messages.ImportWizard_error_on_import, e);
				Throwable cause = e.getCause();

				ErrorDialog.openError(page1.getControl().getShell(), Messages.error_on_import,
						Messages.error_on_import_info, createErrorStatus(cause));
				Thread.currentThread().interrupt();
			}
			fixRuntimeEnvironment();

			// Enable the menu options now we have a platform_home
			ISourceProviderService sourceProvicerSerivce = PlatformUI.getWorkbench()
					.getService(ISourceProviderService.class);

			CommandState commandStateService = (CommandState) sourceProvicerSerivce.getSourceProvider(CommandState.ID);
			commandStateService.setEnabled();
			enableAutoBuild(true);
			enableAutoBuild(isAutoBuildEnabled());
			return Status.OK_STATUS;
		}

		protected void enableAutoBuild(boolean enable) {

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription desc = workspace.getDescription();
			desc.setAutoBuilding(enable);
			try {
				workspace.setDescription(desc);
			} catch (CoreException e) {
				Activator.logError(Messages.ImportPlatformWizard_autobuild_error + enable, e);
			}
		}

		private IStatus createErrorStatus(Throwable e) {
			List<Status> childStatuses = new ArrayList<>();
			StackTraceElement[] stackTraces = e.getStackTrace();
			for (StackTraceElement stackTraceElement : stackTraces) {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, stackTraceElement.toString());
				childStatuses.add(status);
			}
			return new MultiStatus(Activator.PLUGIN_ID, IStatus.ERROR, childStatuses.toArray(new Status[] {}), e.toString(),
					e);
		}
		
		private boolean isAutoBuildEnabled() {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription desc = workspace.getDescription();
			return desc.isAutoBuilding();
		}
	}


	protected void fixRuntimeEnvironment() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM); //$NON-NLS-1$
		IJavaProject javaProject = JavaCore.create(project);
		IVMInstall javaInstall = null;
		try {
			if (javaProject.isOpen()) {
				javaInstall = JavaRuntime.getVMInstall(javaProject);
			}
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		if (javaInstall != null) {
			setHeapSize(javaInstall);
		}
	}

	/**
	 * To be able to run JUnit tests and stand-alone applications the heap sizes
	 * need to be increased
	 * 
	 * @param javaInstall
	 */
	private void setHeapSize(IVMInstall javaInstall) {
		String[] javaVmParams = javaInstall.getVMArguments();
		if (javaVmParams == null || javaVmParams.length == 0) {
			AbstractVMInstall abstractVMInstall = (AbstractVMInstall) javaInstall;
			abstractVMInstall.setVMArgs("-Xmx1500M -XX:MaxPermSize=300M"); //$NON-NLS-1$
		}
	}

	public ImportPlatformWizard() {
		this.page1 = new ImportPlatformPage();
		this.page2 = new AttachSourcesPage(true); // this page is optional: true
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		super.addPages();
		addPage(this.page1);
		addPage(this.page2);
	}

	@Override
	public boolean canFinish() {
		return page1.isPageComplete() && page2.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		if (!page1.validatePage()) {
			// ensure the current page is visible
			getContainer().showPage(page1);
			// render error message
			MessageDialog.openError(getShell(), Messages.ImportWizard_invalid_platform_dir,
					Messages.ImportWizard_invalid_platform_dir_info);
			return false;
		}

		if (!page2.validatePage()) {
			getContainer().showPage(page2);
			MessageDialog.openError(getShell(), Messages.ImportWizard_wrong_src_zip,
					Messages.ImportWizard_wrong_src_zip_info);
			return false;
		}

		importPlatform();
		attachSources();

		return true;
	}

	/**
	 * Attaches the sources.
	 */
	private void attachSources() {
		final File sourceArchive = page2.getSourceFile();

		if (sourceArchive != null) {
			new ProjectSourceJob(sourceArchive).schedule();
		}
	}

	/**
	 * Workhorse to do the actual import of the projects into this workspace.
	 */
	private void importPlatform() {
		final File platformDir = page1.getPlatformDirectory();

		// Set platform home as workspace preference
		try {
			String platformDirStr = platformDir.getCanonicalPath();
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences"); //$NON-NLS-1$
			preferences.put("platform_home", platformDirStr); //$NON-NLS-1$
			preferences.flush();
			new ImportJob().schedule();
		} catch (IOException | BackingStoreException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	protected void importPlatform(IProgressMonitor monitor, File platformDir, boolean fixClasspath,
			boolean removeHybrisGenerator, boolean createWorkingSets, boolean useMultiThread, boolean skipJarScanning)
			throws InvocationTargetException, InterruptedException {
//		try {
//			new Importer().resetProjectsFromLocalExtensions(platformDir, monitor, fixClasspath, removeHybrisGenerator,
//					createWorkingSets, useMultiThread, skipJarScanning);
		new ShellImporter().resetProjectsFromLocalExtensions(platformDir, monitor, fixClasspath, removeHybrisGenerator,
				createWorkingSets, useMultiThread, skipJarScanning);
//		} catch (CoreException e) {
//			Activator.logError(Messages.error_on_import, e);
//			throw new InvocationTargetException(e);
//		} catch (InterruptedException e) {
//			Activator.logError(Messages.error_on_import, e);
//			throw e;
//		}
	}
}
