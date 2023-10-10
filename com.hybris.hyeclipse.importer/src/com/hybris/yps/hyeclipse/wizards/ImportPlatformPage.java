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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.hyeclipse.commons.Constants;

/**
 * TODO refactor, duplicate code in SynchronizePlatformPage 
 */

public class ImportPlatformPage extends WizardPage
{
	public static final String REMOVE_EXISTING_PROJECTS_PREFERENCE = "removeExistingProjectsPreference";
	public static final String REMOVE_HYBRIS_BUILDER_PREFERENCE = "removeHybrisBuilderPreference";
	public static final String FIX_CLASS_PATH_ISSUES_PREFERENCE = "fixClassPathIssuesPreference";
	public static final String CREATE_WORKING_SETS_PREFERENCE = "createWorkingSetsPreference";
	public static final String USE_MULTI_THREAD_PREFERENCE = "useMultiThreadPreference";
	public static final String SKIP_JAR_SCANNING_PREFERENCE = "skipJarScanningPreference";

	private DirectoryFieldEditor	platformDirectoryField;
	private Button						removeExistingProjects;
	private Button fixClasspathIssuesButton;
	private Button removeHybrisItemsXmlGeneratorButton;
	private Button createWorkingSetsButton;
	private Button useMultiThreadButton;
	private Button skipJarScanningButton;

	public ImportPlatformPage()
	{
		super( "Import  Platform" );
		setTitle( "Import  Platform" );
		setDescription( "Import  Platform" );
	}

	public void createControl( Composite parent )
	{
		
		
		final Composite container = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.verticalSpacing = 12;
			container.setLayout(layout);

			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = GridData.FILL;
			container.setLayoutData(data);
		}
		
		this.platformDirectoryField = new DirectoryFieldEditor( "fileSelect", " Platform Home: ", container );
		GridData span2 = new GridData();
		span2.horizontalSpan = 2;
		
		
		platformDirectoryField.getTextControl( container ).addModifyListener( new ModifyListener() 
		{
			@Override
			public void modifyText( ModifyEvent e )
			{				
				setPageComplete(true);
				// let the wizard update it's buttons
				getWizard().getContainer().updateButtons();
				// erase any previous error messages and wait until the performFinish validation happens
				setErrorMessage(null);
			}
		} );
		
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		boolean removeExistingProjectsPref = preferences.getBoolean(REMOVE_EXISTING_PROJECTS_PREFERENCE, true);
		boolean fixClasspathIssuesPref = preferences.getBoolean(FIX_CLASS_PATH_ISSUES_PREFERENCE, true);
		boolean removeHybrisBuilderPref = preferences.getBoolean(REMOVE_HYBRIS_BUILDER_PREFERENCE, true);
		boolean createWorkingSetsPref = preferences.getBoolean(CREATE_WORKING_SETS_PREFERENCE, true);
		boolean useMultiThreadPref = preferences.getBoolean(USE_MULTI_THREAD_PREFERENCE, true);
		boolean skipJarScanningPref = preferences.getBoolean(SKIP_JAR_SCANNING_PREFERENCE, true);

		GridData gdFillHorizontal = new GridData(GridData.FILL_HORIZONTAL);
		gdFillHorizontal.horizontalSpan=2;
		
		GridData gdAlignRight = new GridData(GridData.FILL_HORIZONTAL);
		gdAlignRight.horizontalAlignment = GridData.END;
		
		GridData col3GridData = new GridData(GridData.FILL_HORIZONTAL);
		col3GridData.horizontalSpan = 3;
		
		Label generalOptionsLabel = new Label( container, 0 );
		generalOptionsLabel.setText( "General Options" );
		generalOptionsLabel.setToolTipText("General Options");
		generalOptionsLabel.setLayoutData(col3GridData);
		
		removeExistingProjects = new Button( container, 32 );
		removeExistingProjects.setSelection( removeExistingProjectsPref );
		removeExistingProjects.setLayoutData(gdAlignRight);
		
		Label removeExistingProjectsLabel = new Label( container, 0 );
		removeExistingProjectsLabel.setText( "Remove existing projects" );
		removeExistingProjectsLabel.setToolTipText("Do a clean import removing any existing projects");
		removeExistingProjectsLabel.setLayoutData(gdFillHorizontal);
		
		fixClasspathIssuesButton = new Button( container, 32 );
		fixClasspathIssuesButton.setSelection( fixClasspathIssuesPref );
		fixClasspathIssuesButton.setLayoutData(gdAlignRight);
		
		Label fixClasspathIssuesLabel = new Label( container, 0 );
		fixClasspathIssuesLabel.setText( "Fix classpath issues (recommended)" );
		fixClasspathIssuesLabel.setToolTipText("This will try to fix the project classpath by using the classpath used by the hybris platform and also fixing a number of other common classpath issues");
		fixClasspathIssuesLabel.setLayoutData(gdFillHorizontal);
		
		removeHybrisItemsXmlGeneratorButton = new Button( container, 32 );
		removeHybrisItemsXmlGeneratorButton.setSelection( removeHybrisBuilderPref );
		removeHybrisItemsXmlGeneratorButton.setLayoutData(gdAlignRight);
		
		Label removeHybrisItemsXmlGeneratorLabel = new Label( container, 0 );
		removeHybrisItemsXmlGeneratorLabel.setText( "Remove Hybris Builder (recommended)" );
		removeHybrisItemsXmlGeneratorLabel.setToolTipText("The Hybris Builder will run a build to generate classes on every items.xml save. This generally slows down development and it's usually better to generate the classes by running a build manually");
		removeHybrisItemsXmlGeneratorLabel.setLayoutData(gdFillHorizontal);
		
		createWorkingSetsButton = new Button( container, 32 );
		createWorkingSetsButton.setSelection( createWorkingSetsPref );
		createWorkingSetsButton.setLayoutData(gdAlignRight);
		
		Label createWorkingSetsLabel = new Label( container, 0 );
		createWorkingSetsLabel.setText( "Update Working Sets" );
		createWorkingSetsLabel.setToolTipText("Create from directories of extensions (e.g. ext-commerce)");
		createWorkingSetsLabel.setLayoutData(gdFillHorizontal);

		Label optimzeStartupSettings = new Label( container, 0 );
		optimzeStartupSettings.setText( "Optimize Tomcat Startup Time" );
		optimzeStartupSettings.setToolTipText("Optimize Tomcat Startup Time");
		optimzeStartupSettings.setLayoutData(col3GridData);
		
		useMultiThreadButton = new Button( container, 32 );
		useMultiThreadButton.setSelection( useMultiThreadPref );
		useMultiThreadButton.setLayoutData(gdAlignRight);
		
		Label useMultiThreadLabel = new Label( container, 0 );
		useMultiThreadLabel.setText( "Tomcat Start/Stop with multi-thread" );
		useMultiThreadLabel.setToolTipText("Configure the Tomcat server.xml to set startStopThreads=0");
		useMultiThreadLabel.setLayoutData(gdFillHorizontal);
		
		skipJarScanningButton = new Button( container, 32 );
		skipJarScanningButton.setSelection( skipJarScanningPref );
		skipJarScanningButton.setLayoutData(gdAlignRight);
		
		Label skipJarScanningLabel = new Label( container, 0 );
		skipJarScanningLabel.setText( "Tomcat Start with skipping TLD Jar scanning" );
		skipJarScanningLabel.setToolTipText("Configure the Tomcat catalina.properties to set the value of org.apache.catalina.startup.ContextConfig.jarsToSkip");
		skipJarScanningLabel.setLayoutData(gdFillHorizontal);
		
		setControl( container );
		setPageComplete( false );
	}

	public boolean isRemoveExistingProjects()
	{
		return removeExistingProjects.getSelection();
	}
	
	public boolean isFixClasspath()
	{
		return fixClasspathIssuesButton.getSelection();
	}
	
	public boolean isRemoveHybrisGenerator()
	{
		return removeHybrisItemsXmlGeneratorButton.getSelection();
	}
	
	public boolean isCreateWorkingSets()
	{
		return createWorkingSetsButton.getSelection();
	}
	
	public boolean isUseMultiThread()
	{
		return useMultiThreadButton.getSelection();
	}

	public boolean isSkipJarScanning()
	{
		return skipJarScanningButton.getSelection();
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
	 * @throws  
	 */
	public boolean validatePage()
	{
		File platformDir = getPlatformDirectory();
		
		if (!(platformDir.isDirectory() && 
				(platformDir.getAbsolutePath().endsWith( Constants.PLATFROM ) || 
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
		// persist checkbox selections for next time
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		preferences.putBoolean(FIX_CLASS_PATH_ISSUES_PREFERENCE, fixClasspathIssuesButton.getSelection());
		preferences.putBoolean(REMOVE_HYBRIS_BUILDER_PREFERENCE, removeHybrisItemsXmlGeneratorButton.getSelection());
		preferences.putBoolean(REMOVE_EXISTING_PROJECTS_PREFERENCE, removeExistingProjects.getSelection());
		preferences.putBoolean(CREATE_WORKING_SETS_PREFERENCE, createWorkingSetsButton.getSelection());
		preferences.putBoolean(USE_MULTI_THREAD_PREFERENCE, useMultiThreadButton.getSelection());
		preferences.putBoolean(SKIP_JAR_SCANNING_PREFERENCE, skipJarScanningButton.getSelection());
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException("Could not save preferences", e);
		}
	}
}
