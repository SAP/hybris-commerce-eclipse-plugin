package com.hybris.yps.hyeclipse.wizards;

import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.xml.sax.SAXException;

import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;

import java.io.IOException;
import org.eclipse.core.runtime.IPath;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;

public class ImportExtensionWizard extends Wizard implements IImportWizard {
	private static final String EXTERNAL_PROJECT_SECTION = "ExternalProjectImportWizard";

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
		setWindowTitle("__title__");
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
		try {
			// platformPath
			IPath platformPath = FixProjectsUtils.getPlatformPath();
			if (platformPath == null) {
				System.out.println("ERROR: platformPath not found!");
			} else {
				System.out.println("platformPath=[" + platformPath + "]");
			}
			

			// TODO Handle Checkbox Update Classpath
			
			
			// Handle Checkbox Update localExtensions.xml
			ImportExtensionPage importExtensionPage = getMainPage();
			boolean result = importExtensionPage.createProjects();
			if (result) {
				FixProjectsUtils.updateLocalExtensions(importExtensionPage.getCreatedProjects());
			}
			
		} catch (XPathExpressionException exception) {
			exception.printStackTrace();
		} catch (ParserConfigurationException exception) {
			exception.printStackTrace();
		} catch (SAXException exception) {
			exception.printStackTrace();
		} catch (IOException exception) {
			exception.printStackTrace();
		} catch (TransformerException exception) {
			exception.printStackTrace();
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