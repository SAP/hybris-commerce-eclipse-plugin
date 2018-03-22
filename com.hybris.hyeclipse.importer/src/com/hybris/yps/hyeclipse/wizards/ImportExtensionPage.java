package com.hybris.yps.hyeclipse.wizards;

import java.io.File;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ImportExtensionPage extends WizardPage
{
	public static final String REMOVE_HYBRIS_BUILDER_PREFERENCE = "removeHybrisBuilderPreference";
	public static final String FIX_CLASS_PATH_ISSUES_PREFERENCE = "fixClassPathIssuesPreference";

	private DirectoryFieldEditor	extensionDirectoryFieldEditor;
	private Button fixClasspathIssuesButton;
	private Button removeHybrisItemsXmlGeneratorButton;

	
	public ImportExtensionPage()
	{
		super( "Import [y] Extension" );
		setTitle( "Import [y] Extension" );
		setDescription( "Import [y] Extension" );
	}

	
	public void createControl(Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		{
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			gridLayout.verticalSpacing = 12;
			container.setLayout(gridLayout);

			GridData gridData = new GridData();
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalAlignment = GridData.FILL;
			container.setLayoutData(gridData);
		}
		
		this.extensionDirectoryFieldEditor = new DirectoryFieldEditor( "notUsed", "[y] Extension folder: ", container );
		GridData span2 = new GridData();
		span2.horizontalSpan = 2;
		
		extensionDirectoryFieldEditor.getTextControl( container ).addModifyListener( new ModifyListener() 
		{
			@Override
			public void modifyText(ModifyEvent e)
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
		
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		boolean fixClasspathIssuesPref = preferences.getBoolean(FIX_CLASS_PATH_ISSUES_PREFERENCE, true);
		boolean removeHybrisBuilderPref = preferences.getBoolean(REMOVE_HYBRIS_BUILDER_PREFERENCE, true);

		GridData gridDataFillHorizontal = new GridData(GridData.FILL_HORIZONTAL);
		gridDataFillHorizontal.horizontalSpan = 2;
		
		GridData gridDataAlignRight = new GridData(GridData.FILL_HORIZONTAL);
		gridDataAlignRight.horizontalAlignment = GridData.END;
		
		GridData col3GridData = new GridData(GridData.FILL_HORIZONTAL);
		col3GridData.horizontalSpan = 3;
		
		Label generalOptionsLabel = new Label(container, 0);
		generalOptionsLabel.setText("General Options");
		generalOptionsLabel.setToolTipText("General Options");
		generalOptionsLabel.setLayoutData(col3GridData);
		
		fixClasspathIssuesButton = new Button(container, 32);
		fixClasspathIssuesButton.setSelection(fixClasspathIssuesPref);
		fixClasspathIssuesButton.setLayoutData(gridDataAlignRight);
		
		Label fixClasspathIssuesLabel = new Label(container, 0);
		fixClasspathIssuesLabel.setText("Fix classpath issues (recommended)");
		fixClasspathIssuesLabel.setToolTipText("This will try to fix the project classpath by using the classpath used by the hybris platform and also fixing a number of other common classpath issues");
		fixClasspathIssuesLabel.setLayoutData(gridDataFillHorizontal);
		
		removeHybrisItemsXmlGeneratorButton = new Button(container, 32);
		removeHybrisItemsXmlGeneratorButton.setSelection(removeHybrisBuilderPref);
		removeHybrisItemsXmlGeneratorButton.setLayoutData(gridDataAlignRight);
		
		Label removeHybrisItemsXmlGeneratorLabel = new Label(container, 0);
		removeHybrisItemsXmlGeneratorLabel.setText("Remove Hybris Builder (recommended)");
		removeHybrisItemsXmlGeneratorLabel.setToolTipText("The Hybris Builder will run a build to generate classes on every items.xml save. This generally slows down development and it's usually better to generate the classes by running a build manually");
		removeHybrisItemsXmlGeneratorLabel.setLayoutData(gridDataFillHorizontal);
		
		setControl( container );
		setPageComplete( false );
	}

	
	public boolean isFixClasspath()
	{
		return fixClasspathIssuesButton.getSelection();
	}
	
	public boolean isRemoveHybrisGenerator()
	{
		return removeHybrisItemsXmlGeneratorButton.getSelection();
	}
	

	public File getPlatformDirectory()
	{
		if( extensionDirectoryFieldEditor.getStringValue() != null )
		{
			String platformDirectoryFieldStr = extensionDirectoryFieldEditor.getStringValue();
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
	 * @throws  
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
		
		persistSelections();
		
		return true;
	}

	
	private void persistSelections() {
		// Persist checkbox selections for next time.
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		preferences.putBoolean(FIX_CLASS_PATH_ISSUES_PREFERENCE, fixClasspathIssuesButton.getSelection());
		preferences.putBoolean(REMOVE_HYBRIS_BUILDER_PREFERENCE, removeHybrisItemsXmlGeneratorButton.getSelection());
		try {
			preferences.flush();
		} catch (BackingStoreException exception) {
			throw new IllegalStateException("Could not save preferences", exception);
		}
	}
	
}
