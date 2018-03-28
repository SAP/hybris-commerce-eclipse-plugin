package com.hybris.yps.hyeclipse.wizards;

import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
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

	
	@SuppressWarnings("restriction")
	@Override
	public boolean performFinish() {
		return getMainPage().createProjects();
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