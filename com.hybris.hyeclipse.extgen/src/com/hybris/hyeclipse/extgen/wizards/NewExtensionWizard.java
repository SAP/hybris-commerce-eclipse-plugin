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
package com.hybris.hyeclipse.extgen.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.hybris.hyeclipse.extgen.Activator;
import com.hybris.hyeclipse.extgen.utils.PathUtils;

public class NewExtensionWizard extends Wizard implements INewWizard {

	protected static final String BUILD_CFG = "yplatform_build";
	private NewExtensionWizardPage wizardPage;
	private ISelection currentSelection;
	private boolean autoImport;
//	private ExtensionHolder extension;
	private File saveLocation;
	private String templateName;
	private String packageName;
	private String workingSet;

	public NewExtensionWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		wizardPage = new NewExtensionWizardPage(currentSelection);
		addPage(wizardPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.currentSelection = selection;
	}

	@Override
	public boolean performFinish() {
		getWizardPageConfig();
//		IRunnableWithProgress runnable = new IRunnableWithProgress() {
//			public void run(IProgressMonitor monitor) throws InvocationTargetException {
//				try {
//					generateExtension(monitor, extension.getName(), saveLocation);
//				} catch (CoreException ce) {
//					throw new InvocationTargetException(ce);
//				} finally {
//					monitor.done();
//				}
//			}
//		};
//		try {
//			getContainer().run(true, false, runnable);
//		} catch (InterruptedException ie) {
//			Thread.currentThread().interrupt();
//			return false;
//		} catch (InvocationTargetException ite) {
//			Throwable realException = ite.getTargetException();
//			MessageDialog.openError(getShell(), "Error", realException.getMessage());
//			return false;
//		}
		return true;
	}

	/**
	 * Loads {@link WizardPage} GUI configuration
	 **/
	private void getWizardPageConfig() {
		this.autoImport = wizardPage.getAutoImportCheckboxSelection();
		this.packageName = wizardPage.getPackageName();
		this.templateName = wizardPage.getTemplateName();
		this.saveLocation = wizardPage.getExtensionDirectory();
		String path = PathUtils.getCustomExtensionPath(saveLocation, wizardPage.getExtensionName());
//		this.extension = new ExtensionHolder(path, wizardPage.getExtensionName());
//		this.extension.setCoreModule(wizardPage.getCoreModuleCheckboxSelection());
//		this.extension.setWebModule(wizardPage.getWebModuleCheckboxSelection());
		this.workingSet = wizardPage.getWorkingSet();
		wizardPage.setDefaultExtensionLocation(saveLocation.getAbsolutePath());
	}

	/**
	 * Creates {@link DirectoryFieldEditor} element for page container
	 * 
	 * @param monitor
	 *            progress monitor
	 * @param extensionName
	 *            name of the extension
	 * @param folder
	 *            extension save directory
	 * @throws {@link
	 *             CoreException}
	 */
	private void generateExtension(IProgressMonitor monitor, String extensionName, File folder) throws CoreException {
		monitor.beginTask("Creating " + extensionName, 2);
		if (!folder.exists() || !folder.isDirectory()) {
			throwCoreException("Location \"" + folder + "\" does not exist or is not a directory.");
		}
		try {
			runExtgen(monitor);
			monitor.worked(1);
			// checks if the extension should be moved from the directory in
			// which it was generated
			if (!PathUtils.getDefaultExtensionDirPath().equals(folder.getAbsolutePath())) {
				monitor.beginTask("Moving extension to " + folder, 1);
				moveExtension();
				monitor.worked(1);
			}
			if (autoImport) {
				monitor.beginTask("Importing extension " + extensionName + " to Eclipse", 3);
				importExtension(monitor);
				addToLocalExtensions();
				configureExtensionModules(monitor);
				addExtensionToWorkingSet(monitor);
				monitor.worked(3);
			}
		} catch (InvocationTargetException | IOException e) {
			throwCoreException(e.getMessage());
			Activator.logError("IOException", e);
		}
		monitor.worked(1);
	}

	/**
	 * Builds platform
	 * 
	 * @param monitor
	 *            the progress monitor
	 */
	private void buildPlatform(IProgressMonitor monitor) {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				try {
//					BuildUtils.refreshAndBuild(monitor, BUILD_CFG,
//							ResourcesPlugin.getWorkspace().getRoot().getProject(extension.getName()));
//				} catch (InvocationTargetException e) {
//					throw new IllegalStateException("Failed to build platform", e);
//				}
//			}
//		});
	}

	/**
	 * Configures extension's modules
	 * 
	 * @param monitor
	 *            the progress monitor
	 */
	private void configureExtensionModules(IProgressMonitor monitor) {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				ExtensionUtils.configureModules(monitor, extension);
//			}
//		});
	}

	/**
	 * Imports extension to workspace
	 */
	private void importExtension(IProgressMonitor monitor) {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				try {
//					ExtensionUtils.importExtension(monitor, saveLocation, extension.getName());
//				} catch (CoreException e) {
//					throw new IllegalStateException("Failed to import platform extension", e);
//				}
//			}
//		});
	}

	/**
	 * Moves extension to given directory
	 */
	private void moveExtension() throws IOException {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				try {
//					ExtensionUtils.moveExtension(saveLocation, extension.getName());
//				} catch (IOException e) {
//					throw new IllegalStateException("Failed to move extension to " + saveLocation.getAbsolutePath(), e);
//				}
//			}
//		});
	}

	/**
	 * Adds extension to localextensions.xml
	 */
	private void addToLocalExtensions() throws IOException {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				try {
//					ExtensionUtils.addToLocalExtension(saveLocation, workingSet, extension.getName());
//				} catch (IOException | SAXException | ParserConfigurationException | TransformerException e) {
//					throw new IllegalStateException("Failed to move extension to " + saveLocation.getAbsolutePath(), e);
//				}
//			}
//		});
	}

	/**
	 * Runs ant extgen target
	 * 
	 * @param monitor
	 *            the progress monitor
	 * @throws InvocationTargetException
	 */
	private void runExtgen(IProgressMonitor monitor) throws InvocationTargetException {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				try {
//					ExtensionUtils.runExtgen(monitor, extension.getName(), packageName, templateName);
//				} catch (InvocationTargetException e) {
//					Activator.logError("Failed to generate extension", e);
//					throw new IllegalStateException("Failed to generate extension. Check the console output.", e);
//				}
//			}
//		});
	}

	private void addExtensionToWorkingSet(IProgressMonitor monitor) {
//		Display.getDefault().syncExec(new Runnable() {
//			public void run() {
//				if (workingSet != null && !workingSet.equals(""))
//				{
//					IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(extension.getName());
//					IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSet);
//					if (ws == null)
//					{
//						ws = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(workingSet, new IProject[]{proj});
//					}
//					else
//					{	Set<IProject> projs = new HashSet(Arrays.asList(ws.getElements()));
//						projs.add(proj);
//						ws.setElements(projs.toArray(new IProject[projs.size()]));
//					}
//				}
//			}
//		});
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "com.hybris.hyeclipse.extgen", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public void setCurrentSelection(ISelection currentSelection) {
		this.currentSelection = currentSelection;
	}
}
