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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class SynchronizePlatformPage extends WizardPage
{

	private Button fixClasspathIssuesButton;
	private Button removeHybrisItemsXmlGeneratorButton;
	private Button createWorkingSetsButton;
	private Button useMultiThreadButton;
	private Button skipJarScanningButton;

	public SynchronizePlatformPage()
	{
		super( "Synchronize  Platform" );
		setTitle( "Synchronize  Platform" );
		setDescription( "Synchronize  Platform" );
	}

	public void createControl( Composite parent )
	{
		final Composite container = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.verticalSpacing = 12;
			container.setLayout(layout);

			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = GridData.FILL;
			container.setLayoutData(data);
		}
		
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		boolean fixClasspathIssuesPref = preferences.getBoolean(ImportPlatformPage.FIX_CLASS_PATH_ISSUES_PREFERENCE, true);
		boolean removeHybrisBuilderPref = preferences.getBoolean(ImportPlatformPage.REMOVE_HYBRIS_BUILDER_PREFERENCE, true);
		boolean createWorkingSetsPref = preferences.getBoolean(ImportPlatformPage.CREATE_WORKING_SETS_PREFERENCE, true);
		boolean useMultiThreadPref = preferences.getBoolean(ImportPlatformPage.USE_MULTI_THREAD_PREFERENCE, true);
		boolean skipJarScanningPref = preferences.getBoolean(ImportPlatformPage.SKIP_JAR_SCANNING_PREFERENCE, true);
		
		GridData gdFillHorizontal = new GridData(GridData.FILL_HORIZONTAL);
		gdFillHorizontal.horizontalSpan=2;
		
		GridData gdAlignRight = new GridData(GridData.FILL_HORIZONTAL);
		gdAlignRight.horizontalAlignment = GridData.END;
		
		Label generalOptionsLabel = new Label( container, 0 );
		generalOptionsLabel.setText( "General Options" );
		generalOptionsLabel.setToolTipText("General Options");
		generalOptionsLabel.setLayoutData(gdFillHorizontal);
	
		fixClasspathIssuesButton = new Button( container, 32 );
		fixClasspathIssuesButton.setSelection( fixClasspathIssuesPref );
		fixClasspathIssuesButton.setLayoutData(gdAlignRight);
		
		Label fixClasspathIssuesLabel = new Label( container, 0 );
		fixClasspathIssuesLabel.setText( "Fix classpath issues (recommended)" );
		fixClasspathIssuesLabel.setToolTipText("This will try to fix the project classpath by using the classpath used by the hybris platform and also fixing a number of other common classpath issues");
		//fixClasspathIssuesLabel.setLayoutData(gdFillHorizontal);
		
		removeHybrisItemsXmlGeneratorButton = new Button( container, 32 );
		removeHybrisItemsXmlGeneratorButton.setSelection( removeHybrisBuilderPref );
		removeHybrisItemsXmlGeneratorButton.setLayoutData(gdAlignRight);
		
		Label removeHybrisItemsXmlGeneratorLabel = new Label( container, 0 );
		removeHybrisItemsXmlGeneratorLabel.setText( "Remove Hybris Builder (recommended)" );
		removeHybrisItemsXmlGeneratorLabel.setToolTipText("The Hybris Builder will run a build to generate classes on every items.xml save. This generally slows down development and it's usually better to generate the classes by running a build manually");
		//removeHybrisItemsXmlGeneratorLabel.setLayoutData(gdFillHorizontal);
		
		createWorkingSetsButton = new Button( container, 32 );
		createWorkingSetsButton.setSelection( createWorkingSetsPref );
		createWorkingSetsButton.setLayoutData(gdAlignRight);
		
		Label createWorkingSetsLabel = new Label( container, 0 );
		createWorkingSetsLabel.setText( "Update Working Sets" );
		createWorkingSetsLabel.setToolTipText("Create from directories of extensions (e.g. ext-commerce)");
		//createWorkingSetsLabel.setLayoutData(gdFillHorizontal);

		Label optimzeStartupSettings = new Label( container, 0 );
		optimzeStartupSettings.setText( "Optimize Tomcat Startup Time" );
		optimzeStartupSettings.setToolTipText("Optimize Tomcat Startup Time");
		optimzeStartupSettings.setLayoutData(gdFillHorizontal);
		
		useMultiThreadButton = new Button( container, 32 );
		useMultiThreadButton.setSelection( useMultiThreadPref );
		useMultiThreadButton.setLayoutData(gdAlignRight);
		
		Label useMultiThreadLabel = new Label( container, 0 );
		useMultiThreadLabel.setText( "Tomcat Start/Stop with multi-thread" );
		useMultiThreadLabel.setToolTipText("Configure the Tomcat server.xml to set startStopThreads=0");
		//useMultiThreadLabel.setLayoutData(gdFillHorizontal);
		
		skipJarScanningButton = new Button( container, 32 );
		skipJarScanningButton.setSelection( skipJarScanningPref );
		skipJarScanningButton.setLayoutData(gdAlignRight);
		
		Label skipJarScanningLabel = new Label( container, 0 );
		skipJarScanningLabel.setText( "Tomcat Start with skipping TLD Jar scanning" );
		skipJarScanningLabel.setToolTipText("Configure the Tomcat catalina.properties to set the value of org.apache.catalina.startup.ContextConfig.jarsToSkip");
		//skipJarScanningLabel.setLayoutData(gdFillHorizontal);
		
		setControl( parent );
		setPageComplete( true );
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

	/**
	 * Validation method of this page.
	 * 
	 * @return true if the platform directory is existent and looks correct, false otherwise
	 */
	public boolean validatePage()
	{
		persistSelections();
		return true;
	}

	private void persistSelections() {
		// persist checkbox selections for next time
		Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
		preferences.putBoolean(ImportPlatformPage.FIX_CLASS_PATH_ISSUES_PREFERENCE, fixClasspathIssuesButton.getSelection());
		preferences.putBoolean(ImportPlatformPage.REMOVE_HYBRIS_BUILDER_PREFERENCE, removeHybrisItemsXmlGeneratorButton.getSelection());
		preferences.putBoolean(ImportPlatformPage.CREATE_WORKING_SETS_PREFERENCE, createWorkingSetsButton.getSelection());
		preferences.putBoolean(ImportPlatformPage.USE_MULTI_THREAD_PREFERENCE, useMultiThreadButton.getSelection());
		preferences.putBoolean(ImportPlatformPage.SKIP_JAR_SCANNING_PREFERENCE, skipJarScanningButton.getSelection());

		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException("Could not save preferences", e);
		}
	}
}
