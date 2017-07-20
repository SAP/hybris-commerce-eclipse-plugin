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
		super( "Synchronize [y] Platform" );
		setTitle( "Synchronize [y] Platform" );
		setDescription( "Synchronize [y] Platform" );
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
		
		Label fixClasspathIssuesLabel = new Label( container, 0 );
		fixClasspathIssuesLabel.setText( "Fix classpath issues (recommended)" );
		fixClasspathIssuesLabel.setToolTipText("This will try to fix the project classpath by using the classpath used by the hybris platform and also fixing a number of other common classpath issues");
		
		fixClasspathIssuesButton = new Button( container, 32 );
		fixClasspathIssuesButton.setSelection( fixClasspathIssuesPref );
		fixClasspathIssuesButton.setLayoutData(gdFillHorizontal);
		
		
		
		Label removeHybrisItemsXmlGeneratorLabel = new Label( container, 0 );
		removeHybrisItemsXmlGeneratorLabel.setText( "Remove Hybris Builder (recommended)" );
		removeHybrisItemsXmlGeneratorLabel.setToolTipText("The Hybris Builder will run a build to generate classes on every items.xml save. This generally slows down development and it's usually better to generate the classes by running a build manually");
		removeHybrisItemsXmlGeneratorButton = new Button( container, 32 );
		removeHybrisItemsXmlGeneratorButton.setSelection( removeHybrisBuilderPref );
		removeHybrisItemsXmlGeneratorButton.setLayoutData(gdFillHorizontal);

		Label createWorkingSetsLabel = new Label( container, 0 );
		createWorkingSetsLabel.setText( "Update Working Sets" );
		createWorkingSetsLabel.setToolTipText("Create from directories of extensions (e.g. ext-commerce)");
		createWorkingSetsButton = new Button( container, 32 );
		createWorkingSetsButton.setSelection( createWorkingSetsPref );
		createWorkingSetsButton.setLayoutData(gdFillHorizontal);

		Label useMultiThreadLabel = new Label( container, 0 );
		useMultiThreadLabel.setText( "Tomcat Start/Stop with multi-thread" );
		useMultiThreadLabel.setToolTipText("Configure the Tomcat server.xml to set startStopThreads=0");
		useMultiThreadButton = new Button( container, 32 );
		useMultiThreadButton.setSelection( useMultiThreadPref );
		useMultiThreadButton.setLayoutData(gdFillHorizontal);

		Label skipJarScanningLabel = new Label( container, 0 );
		skipJarScanningLabel.setText( "Tomcat Start with skipping TLD Jar scanning" );
		skipJarScanningLabel.setToolTipText("Configure the Tomcat catalina.properties to set the value of org.apache.catalina.startup.ContextConfig.jarsToSkip");
		skipJarScanningButton = new Button( container, 32 );
		skipJarScanningButton.setSelection( skipJarScanningPref );
		skipJarScanningButton.setLayoutData(gdFillHorizontal);

		setControl( container );
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
