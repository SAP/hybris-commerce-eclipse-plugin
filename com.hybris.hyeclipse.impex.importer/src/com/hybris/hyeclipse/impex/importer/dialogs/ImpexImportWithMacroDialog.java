/**
 * Roy, Cameron
 */
package com.hybris.hyeclipse.impex.importer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.FileFieldEditor;
/**
 * 
 */
public class ImpexImportWithMacroDialog extends TitleAreaDialog {	
	private static final String IMPEX_FILE_NAME = "Impex file name: ";
	private static final String MACRO_FILE_NAME = "Macro file name: ";
	private static final String BROWSE = "Browse...";
	private static final String DIALOG_MESSAGE = "Select a macro file and an impex file name.";
	
	private static final String[] IMPEX_FILE_EXTENSIONS = new String[] { "*.impex" };
	private static final String[] MACRO_FILE_EXTENSIONS = new String[] { "*.impex" };
	
	private String impexFileName = "";
	private FileFieldEditor impexFileFieldEditor;

	private String macroFileName = "";
	private FileFieldEditor macroFileFieldEditor;
	
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ImpexImportWithMacroDialog(Shell parentShell) {
		super(parentShell);
	}

	
	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite area = (Composite) super.createDialogArea(parent);
		
		final Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		final GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginLeft = 10;
		gridLayout.marginRight = 5;
		container.setLayout(gridLayout);
		
		setMessage(DIALOG_MESSAGE);
		
		createMacroFileNameControls(container);
		createImpexFileNameControls(container);
		
		return container;
	}


	/**
	 * Set the title.
	 */
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		shell.setText("Impex Import with Macro");
	}
	
	
	/**
	 * Create impexFileName controls: Label, Text & Browse button.
	 */
	protected void createImpexFileNameControls(Composite container) {
		setImpexFileFieldEditor(new FileFieldEditor("notUsed", IMPEX_FILE_NAME, true, container));
		
		getImpexFileFieldEditor().setChangeButtonText(BROWSE);
		getImpexFileFieldEditor().setEmptyStringAllowed(false);
		getImpexFileFieldEditor().setFileExtensions(IMPEX_FILE_EXTENSIONS);
		getImpexFileFieldEditor().getTextControl(container).setEditable(false);
	}
	
	
	/**
	 * Create macroFileName controls: Label, Text & Browse button.
	 */
	protected void createMacroFileNameControls(Composite container) {
		setMacroFileFieldEditor(new FileFieldEditor("notUsed", MACRO_FILE_NAME, true, container));
		
		getMacroFileFieldEditor().setChangeButtonText(BROWSE);
		getMacroFileFieldEditor().setEmptyStringAllowed(false);
		getMacroFileFieldEditor().setFileExtensions(MACRO_FILE_EXTENSIONS);
		getMacroFileFieldEditor().getTextControl(container).setEditable(false);
	}
	
	
	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	
	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	
	
	@Override
	protected void okPressed() {
		setErrorMessage("");

		if (!getMacroFileFieldEditor().isValid()) {
			setErrorMessage(MACRO_FILE_NAME + getMacroFileFieldEditor().getErrorMessage());
			return;
		}

		if (!getImpexFileFieldEditor().isValid()) {
			setErrorMessage(IMPEX_FILE_NAME + getImpexFileFieldEditor().getErrorMessage());
			return;
		}

		// Valid
		setImpexFileName(getImpexFileFieldEditor().getStringValue());
		setMacroFileName(getMacroFileFieldEditor().getStringValue());
		
		super.okPressed();
	}
	
	
	protected FileFieldEditor getImpexFileFieldEditor() {
		return impexFileFieldEditor;
	}
	
	
	protected void setImpexFileFieldEditor(final FileFieldEditor impexFileFieldEditor) {
		this.impexFileFieldEditor = impexFileFieldEditor;
	}
	
	
	protected FileFieldEditor getMacroFileFieldEditor() {
		return macroFileFieldEditor;
	}
	
	
	protected void setMacroFileFieldEditor(final FileFieldEditor macroFileFieldEditor) {
		this.macroFileFieldEditor = macroFileFieldEditor;
	}
	 
	
	public String getImpexFileName() {
		return impexFileName;
	}
	
	
	public void setImpexFileName(final String impexFileName) {
		this.impexFileName = impexFileName;
	}
	
	
	public String getMacroFileName() {
		return macroFileName;
	}
	
	
	public void setMacroFileName(final String macroFileName) {
		this.macroFileName = macroFileName;
	}
	
}
