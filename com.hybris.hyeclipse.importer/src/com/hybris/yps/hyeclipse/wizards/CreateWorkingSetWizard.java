package com.hybris.yps.hyeclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.hybris.yps.hyeclipse.utils.WorkingSetsUtils;

/**
 *
 *
 */
public class CreateWorkingSetWizard extends Wizard {
	private CreateWorkingSetPage page;

	@Override
	public String getWindowTitle() {
		return "Create Working Sets";
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
			MessageDialog.openError(getShell(), "Not option selected", "Select at least one option");
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
		} catch (InvocationTargetException | InterruptedException e) {
			Throwable t = (e instanceof InvocationTargetException) ? e.getCause() : e;
			MessageDialog.openError(this.page.getControl().getShell(), "Error", t.toString());
		}
		return true;
	}
}
