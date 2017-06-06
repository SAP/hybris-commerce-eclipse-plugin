package com.hybris.yps.hyeclipse.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.hybris.yps.hyeclipse.Activator;

import org.eclipse.core.resources.IBuildConfiguration;

public class EclipseRefreshAndBuildHandler extends AbstractHandler{
	
//	public Object execute(org.eclipse.core.commands.ExecutionEvent evt) throws org.eclipse.core.commands.ExecutionException {
//		
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(evt);
//		WizardDialog wizardDialog = new WizardDialog(window.getShell(), new FixProjectsWizard());
//		wizardDialog.open();
//		return null;
//	}
//	 


	@Override
	public Object execute(ExecutionEvent event) {
	   
	    Job job = new Job("[y] Platform Refresh and Build"){
	        @Override
	        protected IStatus run(IProgressMonitor monitor){
	            
	        	try
	        	{
	        		refreshAndFullBuild(monitor);
	        		monitor.done();
		            return Status.OK_STATUS;
	        	}
	        	catch (Exception e)
	        	{
	        		throw new IllegalStateException("Failed to synchronize with the platform, see workspace logs for details", e);
	        	}
	        	
	        }
	    };
	    job.setUser(true);
	    job.schedule();
	    return null;
	}
	
	private void refreshAndFullBuild(IProgressMonitor monitor) throws InvocationTargetException {
		
		List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		boolean isAutoBuildEnabled = isAutoBuildEnabled();	
		 try
	     {
			 if (isAutoBuildEnabled)
			 {
				 enableAutoBuild(false);
			 }
			
			// refresh all projects one by one  
			for (IProject project : projects)
			{
				if (project.isOpen())
				{
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
			}
			// do a full build
	        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
	        IBuildConfiguration[] platformBuildConfig = { ResourcesPlugin.getWorkspace().newBuildConfig("platform","platform-build")};
	        // build the platform first since other projects depend on it
	        ResourcesPlugin.getWorkspace().build(platformBuildConfig, IncrementalProjectBuilder.FULL_BUILD, true, monitor);
	        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	      }
	      catch (CoreException e)
	      {
	    	Activator.logError("Failed to synchronize with the platform", e);
	        throw new InvocationTargetException(e);
	      }
		 if (isAutoBuildEnabled)
		 {
			 enableAutoBuild(true);
		 }
	}
	
	protected boolean isAutoBuildEnabled()
	{
		IPreferencesService service = Platform.getPreferencesService();
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		String key = "description.autobuilding";
		IScopeContext[] contexts = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE };
		return service.getBoolean(qualifier, key, false, contexts);
	}
	
	protected void enableAutoBuild(boolean enable)
	{
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(qualifier);
		node.putBoolean("description.autobuilding", enable);
		try
		{
			node.flush();
		}
		catch (BackingStoreException e)
		{
			Activator.logError("Failed to enable auto build", e);
			throw new IllegalStateException(e);
		}
	}
}
