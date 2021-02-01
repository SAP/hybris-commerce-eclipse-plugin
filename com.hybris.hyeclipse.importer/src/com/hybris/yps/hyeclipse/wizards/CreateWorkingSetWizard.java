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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.WorkingSetsUtils;

/**
 *
 *
 */
public class CreateWorkingSetWizard extends Wizard {
	private CreateWorkingSetPage page;

	@Override
	public String getWindowTitle() {
		return Messages.CreateWorkingSetWizard_title;
	}

	@Override
	public void addPages() {
		page = new CreateWorkingSetPage();
		addPage(page);
	}

	@Override
	public boolean canFinish() {
		return page.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		if (!page.validatePage()) {
			MessageDialog.openError(getShell(), Messages.CreateWorkingSetWizard_noOptionSelected, Messages.CreateWorkingSetWizard_noOptionSelected_long);
			// and ... abort
			return false;
		}

		final boolean createFromLocalExtensions = page.getCreateFromLocalExtensions().getSelection();
		final boolean createFromExtensionDirectories = page.getCreateFromExtensionDirectories().getSelection();

		IRunnableWithProgress importer = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {

				if (createFromLocalExtensions) {
					WorkingSetsUtils.organizeWorkingSetsFromLocalExtensions(monitor);
				}
				if (createFromExtensionDirectories) {
					WorkingSetsUtils.organizeWorkingSetsFromExtensionDirectories(monitor);
				}
			}
		};

		try {
			new ProgressMonitorDialog(getContainer().getShell()).run(true, false, importer);
		} catch (InvocationTargetException | InterruptedException e) {  // NOSONAR
			Activator.logError(Messages.CreateWorkingSetWizard_importFailed, e);
			MessageDialog.openError(this.page.getControl().getShell(), Messages.CreateWorkingSetWizard_importFailed, e.getMessage());
			return false;
		}
		return true;
	}
}
