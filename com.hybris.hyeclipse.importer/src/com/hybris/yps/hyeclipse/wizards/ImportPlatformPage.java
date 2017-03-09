package com.hybris.yps.hyeclipse.wizards;

import java.io.File;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ImportPlatformPage extends WizardPage
{
	private DirectoryFieldEditor	platformDirectoryField;
	private Button						removeExistingProjects;

	public ImportPlatformPage()
	{
		super( "Import [y] Platform" );
		setTitle( "Import [y] Platform" );
		setDescription( "Import [y] Platform" );
	}

	public void createControl( Composite parent )
	{
		Composite container = new Composite( parent, SWT.NONE );
		container.setLayout( new GridLayout( 2, false ) );
		this.platformDirectoryField = new DirectoryFieldEditor( "fileSelect", "[y] Platform Home: ", container );
		
		platformDirectoryField.getTextControl( container ).addModifyListener( new ModifyListener() 
		{
			@Override
			public void modifyText( ModifyEvent e )
			{
				// no validation just yet, as this even gets triggered:
				// (1) copy and paste: once
				// (2) manual typing: once per character entered
				// (3) per change button - selection: once
				// so because of #2: need to do validation when submitting the wizard (in it's performFinish())
				
				setPageComplete(true);
				// let the wizard update it's buttons
				getWizard().getContainer().updateButtons();
				// erase any previous error messages and wait until the performFinish validation happens
				setErrorMessage(null);
			}
		} );
		
		Label removeExistingProjectsLabel = new Label( container, 0 );
		removeExistingProjectsLabel.setText( "Remove existing projects" );
		removeExistingProjects = new Button( container, 32 );
		removeExistingProjects.setSelection( true );

		setControl( container );
		setPageComplete( false );
	}

	public boolean isRemoveExistingProjects()
	{
		return removeExistingProjects.getSelection();
	}

	public File getPlatformDirectory()
	{
		if( platformDirectoryField.getStringValue() != null )
		{
			String platformDirectoryFieldStr = platformDirectoryField.getStringValue();
			platformDirectoryFieldStr = platformDirectoryFieldStr.trim();
			return new File(platformDirectoryFieldStr);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Validation method of this page.
	 * 
	 * @return true if the platform directory is existent and looks correct, false otherwise
	 */
	public boolean validatePage()
	{
		File platformDir = getPlatformDirectory();
		
		if (!(platformDir.isDirectory() && 
				(platformDir.getAbsolutePath().endsWith( "platform" ) || 
						platformDir.getAbsolutePath().endsWith( "platform/" ) || 
						platformDir.getAbsolutePath().endsWith( "platform\\" ))))
		{
			setErrorMessage( "Invalid platform directory" );
			return false;
		}
		
		File envProps = new File( platformDir, "env.properties" );
		File activeRoleEnvProps = new File( platformDir, "active-role-env.properties" );
		
		// File platformHomeProps = new File(platformDir, "platformhome.properties");
		if( !envProps.exists() && !activeRoleEnvProps.exists() )
		{
			setErrorMessage( "Plaform has not been build yet, run \"ant all\" from the platform dir" );
			return false;
		}
		
		//clumsy to set to null rather than clearErrorMessage() similar
		setErrorMessage(null);
		
		return true;
	}
}
