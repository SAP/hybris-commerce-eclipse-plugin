package com.hybris.yps.hyeclipse.wizards;

import java.io.File;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Page for a wizard to present a selector for a source archive. Includes validation logic of the selected file.
 * 
 * 
 * @author mheuer
 *
 */
public class AttachSourcesPage extends WizardPage 
{

	private FileFieldEditor sourceFileField;
	private Composite container;
	private boolean isOptional;

	public AttachSourcesPage(boolean isOptional) 
	{
		super("Attach Sources");
		setTitle("Attach Sources");
		final String prefix = isOptional ? "Optional: " : "";
		setDescription(prefix + "Attach the sources from a hybris source archive to the hybris binary jars. I.e. the current workspace's *server.jars.");
		this.isOptional = isOptional;
	}

	@Override
	public void createControl(Composite parent) 
	{
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		this.sourceFileField = new FileFieldEditor("fileSelect", "[y] Source Archive:", container);
		sourceFileField.setFileExtensions( new String[]{"*.zip"} );
		// Set the starting path to the downloads folder or home dir of the current user.
		sourceFileField.setFilterPath( new File(System.getProperty("user.home")) );

		sourceFileField.getTextControl( container ).addModifyListener( new ModifyListener() 
		{
			@Override
			public void modifyText( ModifyEvent e )
			{
				// no validation just yet, as this even gets triggered:
				// (1) copy and paste: once
				// (2) manual typing: once per character entered
				// (3) per change button - selection: once
				// so because of #2: need to do validation when submitting the wizard (in it's performFinish())
				// TODO: there must be a better way, but the FocusListener does not see when the selection is done via the browse button
				
				setErrorMessage(null);
				setPageComplete(true);
				// let the wizard update it's buttons
				getWizard().getContainer().updateButtons();
			}
		} );
				
		// required to avoid an error in the system
		setControl(container);
		// if this is an optional page, we set this page to complete right away.
		setPageComplete(isOptional);
	}

	
	/**
	 * Check if the contents of the page are valid.
	 * 
	 * Remember that the source file could be optional. So any checks will only apply if there is actually anything specified.
	 * 
	 * @return true if the picked archive exists and is readable. False otherwise.
	 */
	public boolean validatePage() 
	{
		File sourceArchive = getSourceFile();
		
		// if it is optional: valid if we don't have a value.
		if (isOptional && sourceFileField.getStringValue().trim().isEmpty()) 
		{
			return true;
		}

		// otherwise the file should exist and should be readable.
		if (!sourceArchive.exists() || !sourceArchive.isFile() || !sourceArchive.canRead())
		{
			setErrorMessage("Selected file does not exist or is not readable.");
			return false;
		}
		
		setErrorMessage(null);
		return true;
	}

	/**
	 * Getter for the payload of this page.
	 * 
	 * @return File or null if nothing was selected/entered.
	 */
	public File getSourceFile() 
	{
		if (!sourceFileField.getStringValue().trim().isEmpty())
		{
			return new File(sourceFileField.getStringValue());
		}

		return null;
	}

}
