/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/** Creates an ImpEx file without requiring a Commerce project or language server. */
public final class NewImpexWizard extends Wizard implements INewWizard {

	private IWorkbench workbench;
	private IStructuredSelection selection;
	private WizardNewFileCreationPage page;

	/** Creates a wizard with background progress support. */
	public NewImpexWizard() {
		setWindowTitle(Messages.wizardTitle);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}

	@Override
	public void addPages() {
		page = new WizardNewFileCreationPage("impexFile", selection) { //$NON-NLS-1$
			@Override
			protected void createAdvancedControls(final Composite parent) {
				// Linked/external file creation is intentionally outside this wizard's scope.
			}

			@Override
			protected IStatus validateLinkedResource() {
				return Status.OK_STATUS;
			}

			@Override
			protected boolean validatePage() {
				if (!super.validatePage()) {
					return false;
				}
				final IStatus status = ImpexFileCreator.validateFileName(getFileName());
				if (!status.isOK()) {
					setErrorMessage(status.getMessage());
					return false;
				}
				return true;
			}
		};
		page.setTitle(Messages.wizardTitle);
		page.setDescription(Messages.wizardDescription);
		page.setFileName(ImpexFileCreator.DEFAULT_FILE_NAME);
		page.setAllowExistingResources(false);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		final IPath containerPath = page.getContainerFullPath();
		final String fileName = page.getFileName();
		final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
		final IContainer destination = resource instanceof IContainer ? (IContainer) resource : null;
		final AtomicReference<IFile> result = new AtomicReference<>();
		try {
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(Messages.creatingFile, 1);
				try {
					result.set(ImpexFileCreator.create(destination, fileName, monitor));
				} catch (final CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			});
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getTargetException();
			if (cause instanceof OperationCanceledException) {
				return false;
			}
			final IStatus status = cause instanceof CoreException ? ((CoreException) cause).getStatus()
					: new Status(IStatus.ERROR, ImpexFileCreator.PLUGIN_ID, Messages.creationError, cause);
			ErrorDialog.openError(getShell(), Messages.creationError, null, status);
			return false;
		}
		final IFile file = result.get();
		if (file == null) {
			return false;
		}
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window != null) {
			BasicNewResourceWizard.selectAndReveal(file, window);
		}
		if (window != null && window.getActivePage() != null) {
			try {
				IDE.openEditor(window.getActivePage(), file, ImpexFileCreator.EDITOR_ID, true);
			} catch (final PartInitException e) {
				// Creation succeeded; closing the wizard avoids accidentally trying to create the file again.
				ErrorDialog.openError(getShell(), Messages.openingError, null, e.getStatus());
			}
		}
		return true;
	}
}
