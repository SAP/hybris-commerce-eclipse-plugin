package com.hybris.yps.hyeclipse.handlers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.prefs.Preferences;

import com.hybris.hyeclipse.ytypesystem.Activator;
import com.hybris.yps.hyeclipse.utils.Importer;
import com.hybris.yps.hyeclipse.utils.ProjectSourceUtil;

public class FixProjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {

		final Shell activeShell = HandlerUtil.getActiveShell(event);
		
		IRunnableWithProgress runner = new IRunnableWithProgress() {
			public void run(IProgressMonitor ipm) throws InvocationTargetException {
				try {
					File platformHome = null;
					Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
					String platformHomeStr = preferences.get("platform_home", null);
					if (platformHomeStr == null) {
						IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
						IPath platformProjectPath = platformProject.getLocation();
						if (platformProjectPath != null) {
							platformHome = platformProjectPath.toFile();
						}
					}
					else {
						platformHome = new File(platformHomeStr);
					}
					new Importer().resetProjectsFromLocalExtensions(platformHome, ipm);
				}
				catch (Exception ce) {
					throw new InvocationTargetException(ce);
				}
				finally {
					ipm.done();
				}
			}
		};
		try {
			new ProgressMonitorDialog( activeShell ).run( true, false, runner );
		}
		catch (InterruptedException ie) {
			return false;
		}
		catch (InvocationTargetException ite) {
			Throwable realException = ite.getTargetException();
			MessageDialog.openError(activeShell, "Failed to synchronize with the platform", realException.getMessage() + "\nFor more details see the workspace logs in <workspace>/.metadata/.log");
			return false;
		}
		return null;
	}
}
