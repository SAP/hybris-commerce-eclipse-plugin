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

/**
 * 
 */
public class ImpexImportWithMacroDialog extends TitleAreaDialog {	
	private static final String IMPEX_FILE_NAME = "Impex file name:";
	private static final String MACRO_FILE_NAME = "Macro file name:";
	
	private String impexFileName = "";
	private Text impexFileNameText;

	private String macroFileName = "";
	private Text macroFileNameText;
	
	
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

		final GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginLeft = 10;
		gridLayout.marginRight = 5;
		container.setLayout(gridLayout);
		
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
		final Label impexFileNameLabel = new Label(container, SWT.NONE);
		impexFileNameLabel.setText(IMPEX_FILE_NAME);
		
		setImpexFileNameText(new Text(container, SWT.BORDER));
		getImpexFileNameText().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		getImpexFileNameText().setText(getImpexFileName());
		
		getImpexFileNameText().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent modifyEvent) {
				final Text text = (Text) modifyEvent.getSource();
				setImpexFileName(text.getText());
			}
		});
		
		// TODO: Broswe button
	}
	
	
	/**
	 * Create macroFileName controls: Label, Text & Browse button.
	 */
	protected void createMacroFileNameControls(Composite container) {
		final Label macroFileNameLabel = new Label(container, SWT.NONE);
		macroFileNameLabel.setText(MACRO_FILE_NAME);
		
		setMacroFileNameText(new Text(container, SWT.BORDER));
		getMacroFileNameText().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		getMacroFileNameText().setText(getMacroFileName());		
		
		getMacroFileNameText().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent modifyEvent) {
				final Text text = (Text) modifyEvent.getSource();
				setMacroFileName(text.getText());
			}
		});

		// TODO: Browse button
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
		setImpexFileName(impexFileName);
		setMacroFileName(macroFileName);
		
		super.okPressed();
	}
	
	
	protected Text getImpexFileNameText() {
		return impexFileNameText;
	}
	
	
	protected void setImpexFileNameText(final Text impexFileNameText) {
		this.impexFileNameText = impexFileNameText;
	}
	
	
	protected Text getMacroFileNameText() {
		return macroFileNameText;
	}
	
	
	protected void setMacroFileNameText(final Text macroFileNameText) {
		this.macroFileNameText = macroFileNameText;
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
