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
package com.hybris.hyeclipse.tsv.wizards;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.hybris.hyeclipse.commons.validation.ValidationResult;
import com.hybris.hyeclipse.commons.validation.ValidationService;
import com.hybris.hyeclipse.commons.validation.ValidationXml;
import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsv.Messages;
import com.hybris.hyeclipse.tsv.editors.TSVResultsInput;
import com.hybris.hyeclipse.tsv.editors.TSVResultsStorage;

public class RunTSVWizard extends Wizard implements INewWizard {
	private RunTSVWizardPage page;
	private ISelection selection;
	private String resultsString;

	public RunTSVWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new RunTSVWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {

		final File scanDir = page.getScanDirectory();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(scanDir, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof OperationCanceledException) {
				return false;
			}
			Activator.logError(Messages.RunTSVWizard_error, e);
			MessageDialog.openError(getShell(), Messages.RunTSVWizard_error, e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method...
	 */
	private void doFinish(File scanDir, IProgressMonitor monitor) throws CoreException {

		monitor.beginTask(Messages.RunTSVWizard_taskCreating, 3);
		monitor.worked(1);

		try {
			runTSVAnalysis(scanDir, monitor);
			monitor.worked(1);

			monitor.setTaskName(Messages.RunTSVWizard_taskOpening);
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {

					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

					try {
						IStorage storage = new TSVResultsStorage(getResultsString(), new Path("garbage")); //$NON-NLS-1$
						IStorageEditorInput input = new TSVResultsInput(storage, Messages.RunTSVWizard_TSVTitle);
						IDE.openEditor(page, input, "com.hybris.hyeclipse.tsv.editors.TSVEditor", true); //$NON-NLS-1$
					} catch (PartInitException e) {
						Activator.logError(Messages.RunTSVWizard_error, e);
					}
				}
			});
			monitor.worked(1);
		} catch (RuntimeException re) {
			if (re instanceof OperationCanceledException) {
				throw re;
			}
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.RunTSVWizard_error, re));
		}

	}

	private void runTSVAnalysis(final File scanDir, final IProgressMonitor monitor) throws CoreException {
		try {
			final List<ValidationResult> results = new ArrayList<>();
			final ValidationService service = new ValidationService();
			for (final File file : ValidationService.findItemsFiles(scanDir, monitor)) {
				results.addAll(service.analyze(file, monitor));
			}
			setResultsString(ValidationXml.report(results));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.RunTSVWizard_error, e));
		}

	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public String getResultsString() {
		return resultsString;
	}

	public void setResultsString(String resultsString) {
		this.resultsString = resultsString;
	}

	public static String fromStream(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder out = new StringBuilder();
		String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
		String line;
		while ((line = reader.readLine()) != null) {
			out.append(line);
			out.append(newLine);
		}
		return out.toString();
	}

}
