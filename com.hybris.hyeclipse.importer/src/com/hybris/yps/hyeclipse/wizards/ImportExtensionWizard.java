package com.hybris.yps.hyeclipse.wizards;

import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.xml.sax.SAXException;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;
import com.hybris.yps.hyeclipse.utils.Importer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;

public class ImportExtensionWizard extends Wizard implements IImportWizard {
	private static final String EXTERNAL_PROJECT_SECTION = "ExternalProjectImportWizard";

	private static final String IMPORT_EXTENSIONS_TITLE = "Import Extension(s)";
	
	private ImportExtensionPage mainPage = null;
	private String initialPath = null;
	private IStructuredSelection currentSelection = null;

	
	public ImportExtensionWizard() {
		this(null);
	}

	
	public ImportExtensionWizard(String initialPath) {
		super();
		setInitialPath(initialPath);
		setNeedsProgressMonitor(true);
		IDialogSettings workbenchSettings = IDEWorkbenchPlugin.getDefault().getDialogSettings();
		
		IDialogSettings wizardSettings = workbenchSettings.getSection(EXTERNAL_PROJECT_SECTION);
		if (wizardSettings == null) {
			wizardSettings = workbenchSettings.addNewSection(EXTERNAL_PROJECT_SECTION);
		}
		setDialogSettings(wizardSettings);
	}
	
	
	@Override
	public void addPages() {
		super.addPages();
		setMainPage(new ImportExtensionPage("importExtensionPage", getInitialPath(), getCurrentSelection()));
		addPage(getMainPage());
	}
  
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		setWindowTitle(IMPORT_EXTENSIONS_TITLE);
		setDefaultPageImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/importproj_wiz.png"));
		setCurrentSelection(currentSelection);
	}

	
	@SuppressWarnings("restriction")
	@Override
	public boolean performCancel() {
		getMainPage().performCancel();
		return true;
	}

	
	@Override
	public boolean performFinish() {
		// get platformPath
		IPath platformPath = FixProjectsUtils.getPlatformPath();
		if (platformPath == null) {
			Activator.logError("ERROR: platformPath not found!", null);
			return false;
		} 
		
		// Import the projects.
		ImportExtensionPage importExtensionPage = getMainPage();
		boolean projectsCreated = importExtensionPage.createProjects();
		if (!projectsCreated) {
			return false;
		}
		
		// Update localextensions.xml
		if (importExtensionPage.getUpdateLocalExtensions()) {
			try {
				FixProjectsUtils.updateLocalExtensions(importExtensionPage.getCreatedProjects());
			} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException | TransformerException exception) {
				exception.printStackTrace();
				return false;
			}
		}
		
		// Fix classpath
		File platformDir = new File(platformPath.toString());
		IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
			
			public void run(IProgressMonitor progressMonitor ) throws InvocationTargetException {
				if (importExtensionPage.getFixClasspath()) {
					try {
						new Importer().resetProjectsFromLocalExtensions(platformDir, progressMonitor, importExtensionPage.getFixClasspath(), false, false, false, false);
						
					} catch (CoreException exception) {
						exception.printStackTrace();
					}
				}
			}
		};

		
		try {
			new ProgressMonitorDialog(getContainer().getShell()).run(true, false, runnableWithProgress);
		}
		catch( InvocationTargetException | InterruptedException exception) {
			Activator.logError("Failed to import the extension(s)", exception);
			Throwable throwable = (exception instanceof InvocationTargetException) ? exception.getCause() : exception;
			MessageDialog.openError(getMainPage().getControl().getShell(), "Error", throwable.toString());
			return false;
		}
		
		return true;
	}
	
	
	protected IStructuredSelection getCurrentSelection() {
		return currentSelection;
	}
	
	
	protected void setCurrentSelection(final IStructuredSelection currentSelection) {
		this.currentSelection = currentSelection;
		
	}
	
	
	protected String getInitialPath() {
		return initialPath;
	}
	
	
	protected void setInitialPath(final String initialPath) {
		this.initialPath = initialPath;
	}
	
	protected ImportExtensionPage getMainPage() {
		return mainPage;
	}
	
	
	protected void setMainPage(final ImportExtensionPage mainPage) {
		this.mainPage = mainPage;
	}
	
}