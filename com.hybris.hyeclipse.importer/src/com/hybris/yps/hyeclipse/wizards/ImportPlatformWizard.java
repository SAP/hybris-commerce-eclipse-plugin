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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.CommandState;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;
import com.hybris.yps.hyeclipse.utils.Importer;
import com.hybris.yps.hyeclipse.utils.ProjectSourceUtil;

/**
 * Wizard to walk the user through importing all projects from a given platform directory.
 * 
 * @author brendan
 *
 */
public class ImportPlatformWizard extends Wizard implements IImportWizard
{
	private ImportPlatformPage	page1;
	private AttachSourcesPage	page2;

	public ImportPlatformWizard()
	{
		this.page1 = new ImportPlatformPage();
		this.page2 = new AttachSourcesPage(true); // this page is optional: true
	}

	@Override
	public void init( IWorkbench workbench, IStructuredSelection selection )
	{
		setNeedsProgressMonitor( true );
	}

	@Override
	public void addPages()
	{
		super.addPages();
		addPage(this.page1);
		addPage(this.page2);
	}

	@Override
	public boolean canFinish() 
	{
		return page1.isPageComplete() && page2.isPageComplete();
	}
	
	@Override
	public boolean performFinish()
	{
		// MHE: this lazy validation business is a bit sketchy IMHO
		if (!page1.validatePage()) 
		{
			// ensure the current page is visible
			getContainer().showPage( page1 );
			// render error message
			MessageDialog
					.openError(
							getShell(),
							"Invalid platform directory",
							"The platform directory is invalid. Please set the location to a valid directory (e.g. <path>/hybris/bin/platform) and make sure the platform has been built already (\"ant all\")." );
			//TODO: set focus to the input field in question
			
			// abort
			return false;
		}
		
		if (!page2.validatePage()) 
		{
			// ensure page 2 is visible so we can hone in on the source of the validation error.
			getContainer().showPage( page2 );
			MessageDialog
			.openError(
					getShell(),
					"Unreadable or non-existing file specified",
					"Please make sure the archive you selected is readable to the current user and exists." );
			
			//TODO: set focus to the input field in question
			
			// and ... abort
			return false;
		}
		
		importPlatform();
		attachSources();
		
		return true;
	}


	/**
	 * Attaches the sources.
	 */
	private void attachSources()
	{
		final File sourceArchive = page2.getSourceFile();	
		
		if (sourceArchive == null) 
		{
			return; //nothing to do
		}
		
		IRunnableWithProgress runner = ProjectSourceUtil.getRunner(sourceArchive);

		try
		{
			new ProgressMonitorDialog( getContainer().getShell() ).run( true, false, runner );

		}
		catch( InvocationTargetException | InterruptedException e )
		{
			Throwable t = (e instanceof InvocationTargetException) ? e.getCause() : e;
			MessageDialog.openError( getShell(), "Error attaching sources", t.toString() );
		}
		
	}

	/**
	 * Workhorse to do the actual import of the projects into this workspace.
	 */
	private void importPlatform()
	{
		boolean autobuildEnabled = isAutoBuildEnabled();
		enableAutoBuild( false );
		final boolean removeExistingProjects = page1.isRemoveExistingProjects();
		final boolean fixClasspath = page1.isFixClasspath();
		final boolean removeHybrisBuilder = page1.isRemoveHybrisGenerator();
		final boolean createWorkingSet = page1.isCreateWorkingSets();
		final boolean useMultiThread = page1.isUseMultiThread();
		final boolean skipJarScanning = page1.isSkipJarScanning();

		final File platformDir = page1.getPlatformDirectory();
		
		//Set platform home as workspace preference
		try {
			String platformDirStr = platformDir.getCanonicalPath();
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			preferences.put("platform_home", platformDirStr);
			preferences.flush();
		}
		catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
		
		IRunnableWithProgress importer = new IRunnableWithProgress()
		{
			public void run( IProgressMonitor monitor ) throws InvocationTargetException
			{
				List<IProject> projects = Arrays.asList( ResourcesPlugin.getWorkspace().getRoot().getProjects() );
				if( removeExistingProjects && projects != null && (projects.size() > 0) )
				{
					monitor.setTaskName( "Removing projects" );
					monitor.beginTask( "Removing projects", projects.size() );
					int progress = 0;
					for( IProject project: projects )
					{
						try
						{
							if( FixProjectsUtils.isAHybrisExtension( project ) )
							{
								project.delete( false, true, monitor );
							}
						}
						catch( CoreException e )
						{
							throw new InvocationTargetException( e );
						}
						progress++;
						monitor.worked( progress );
					}
				}
				importPlatform( monitor, projects, platformDir, fixClasspath, removeHybrisBuilder, createWorkingSet, useMultiThread, skipJarScanning);
				// fix JRE settings to make it easier to run tests
				fixRuntimeEnvironment( platformDir.getAbsolutePath() );
				
				//Enable the menu options now we have a platform_home
				ISourceProviderService sourceProvicerSerivce = 
				        (ISourceProviderService)PlatformUI.getWorkbench().getService(
				                ISourceProviderService.class);
				
				CommandState commandStateService = (CommandState) sourceProvicerSerivce.getSourceProvider(CommandState.ID);
				commandStateService.setEnabled();
			}
		};
		try
		{
			new ProgressMonitorDialog( getContainer().getShell() ).run( true, false, importer );
		}
		catch( InvocationTargetException | InterruptedException e )
		{
			Activator.logError("Failed to import the platform",e);
			Throwable t = (e instanceof InvocationTargetException) ? e.getCause() : e;
			MessageDialog.openError( this.page1.getControl().getShell(), "Error", t.toString() );
			enableAutoBuild( autobuildEnabled );
		}
		enableAutoBuild( autobuildEnabled );
	}

	protected boolean isAutoBuildEnabled()
	{
		IPreferencesService service = Platform.getPreferencesService();
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		String key = "description.autobuilding";
		IScopeContext[] contexts = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE};
		return service.getBoolean( qualifier, key, false, contexts );
	}

	protected void enableAutoBuild( boolean enable )
	{
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode( qualifier );
		node.putBoolean( "description.autobuilding", enable );
		try
		{
			node.flush();
		}
		catch( BackingStoreException e )
		{
			throw new IllegalStateException( e );
		}
	}

	protected void importPlatform( IProgressMonitor monitor, List<IProject> projects, File platformDir , boolean fixClasspath, boolean removeHybrisGenerator, boolean createWorkingSets, boolean useMultiThread, boolean skipJarScanning) throws InvocationTargetException
	{
		try
		{
			new Importer().resetProjectsFromLocalExtensions( platformDir, monitor, fixClasspath, removeHybrisGenerator, createWorkingSets, useMultiThread, skipJarScanning);
		}
		catch( CoreException e )
		{
			Activator.logError("Failed to import the platform",e);
			throw new InvocationTargetException( e );
		}
	}

	protected void fixRuntimeEnvironment( String platformDir )
	{
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( "platform" );
		IJavaProject javaProject = JavaCore.create( project );
		IVMInstall javaInstall = null;
		try
		{
			if(javaProject.isOpen())
			{
			javaInstall = JavaRuntime.getVMInstall( javaProject );
		}
		}
		catch( CoreException e )
		{
			throw new IllegalStateException( e );
		}
		if( javaInstall != null )
		{
			setHeapSize( javaInstall );
		}
	}

	/**
	 * To be able to run JUnit tests and standalone applications the heap sizes need to be increased
	 * 
	 * @param javaInstall
	 */
	private void setHeapSize( IVMInstall javaInstall )
	{
		String[] javaVmParams = javaInstall.getVMArguments();
		if( javaVmParams == null || javaVmParams.length == 0 )
		{
			AbstractVMInstall abstractVMInstall = (AbstractVMInstall) javaInstall;
			abstractVMInstall.setVMArgs("-Xmx1500M -XX:MaxPermSize=300M");
		}
	}
}
