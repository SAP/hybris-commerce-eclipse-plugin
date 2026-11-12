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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.jobs.ImportJob;

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

		return true;
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
			ImportJobData data = new ImportJobData(page1.isRemoveExistingProjects(), page1.isFixClasspath(),
					page1.isRemoveHybrisGenerator(), page1.isCreateWorkingSets(), page1.isUseMultiThread(),
					page1.isSkipJarScanning(), page1.getPlatformDirectory());
			new ImportJob(data, page2.getSourceFile()).schedule();
		} catch (IOException | BackingStoreException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
}
