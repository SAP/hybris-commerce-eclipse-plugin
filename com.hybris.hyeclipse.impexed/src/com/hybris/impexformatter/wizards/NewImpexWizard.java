package com.hybris.impexformatter.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;

import com.hybris.impexformatter.Activator;

public class NewImpexWizard extends Wizard implements INewWizard {
	
	private NewImpexWizardPage wizardPage;
	private ISelection currentSelection;

	public NewImpexWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		wizardPage = new NewImpexWizardPage(currentSelection);
		addPage(wizardPage);
	}

	public void setCurrentSelection(ISelection selection) {
		this.currentSelection = selection;
	}

	@Override
	public boolean performFinish() {
		final String containerName = wizardPage.getContainerName();
		final String fileName = wizardPage.getFileName();
		IRunnableWithProgress irwp = new IRunnableWithProgress() {
			public void run(IProgressMonitor ipm) throws InvocationTargetException {
				try {
					createSampleFile(ipm, containerName, fileName);
				}
				catch (CoreException ce) {
					throw new InvocationTargetException(ce);
				}
				finally {
					ipm.done();
				}
			}
		};
		try {
			getContainer().run(true, false, irwp);
		}
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return false;
		}
		catch (InvocationTargetException ite) {
			Throwable realException = ite.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void createSampleFile(IProgressMonitor monitor, String containerName, String fileName) throws CoreException {
		
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream();
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			}
			else {
				file.create(stream, true, monitor);
			}
			if (stream != null) {
				stream.close();
			}
		} 
		catch (IOException e) {
			Activator.logError("IOException", e);
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(IDE.EDITOR_ID_ATTR, "com.hybris.impexformatter.editors.ImpexPageEditor");
					IMarker marker = file.createMarker(IMarker.TEXT);
					marker.setAttributes(map);
					IDE.openEditor(page, marker, true);
					marker.delete();
				} 
				catch (PartInitException e) {
					Activator.logError("PartInitException", e);
				} 
				catch (CoreException e) {
					Activator.logError("CoreException", e);
				}
			}
		});
		monitor.worked(1);
	}

	private InputStream openContentStream() {
		String contents = "# USE CTRL-SPACE for type-ahead support\n";
		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "impexFormatter", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.currentSelection = selection;
	}
	
}