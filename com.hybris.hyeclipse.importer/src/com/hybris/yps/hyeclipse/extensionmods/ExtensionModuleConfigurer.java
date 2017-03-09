package com.hybris.yps.hyeclipse.extensionmods;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;

public class ExtensionModuleConfigurer {

	private Set<ExtensionHolder> allPlatformExtensions;
	private Set<IExtensionListViewer> changeListeners = new HashSet<IExtensionListViewer>();
	private Shell shell;

	public ExtensionModuleConfigurer(Composite composite) {
		super();
		this.shell = composite.getShell();
		this.initData(shell);
	}

	private void initData(Shell shell) {
		IRunnableWithProgress op = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("Loading module info", 10);

					Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
					String platformHomeStr = preferences.get("platform_home", null);
					File platformHome = null;
					if (platformHomeStr == null) {
						IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
						IPath platformProjectPath = platformProject.getLocation();
						if (platformProjectPath != null) {
							platformHome = platformProjectPath.toFile();
							platformHomeStr = platformHome.getAbsolutePath();
						}
					} else {
						platformHome = new File(platformHomeStr);
					}

					if (platformHome != null) {
						monitor.worked(1);
						allPlatformExtensions = FixProjectsUtils
								.getAllExtensionsForPlatform(platformHome.getAbsolutePath());
						monitor.worked(9);
					} else {
						allPlatformExtensions = null;
						monitor.worked(10);
					}
				} finally {
					monitor.done();
				}
			}

		};

		try {
			new ProgressMonitorDialog(shell).run(true, false, op);
		} catch (InvocationTargetException e) {
			Activator.logError("InvocationTargetException", e);
		} catch (InterruptedException e) {
			Activator.logError("InterruptedException", e);
		}
	}

	/**
	 * Return the collection of extensions
	 */
	public Set<ExtensionHolder> getAllPlatformExtensions() {
		return allPlatformExtensions;
	}

	public void extensionChanged(final ExtensionHolder extension) {

		Iterator<IExtensionListViewer> iterator = changeListeners.iterator();
		while (iterator.hasNext())
			((IExtensionListViewer) iterator.next()).updateExtension(extension);

		IRunnableWithProgress op = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				ExtensionModuleTrimmer.configureExtension(monitor, extension);
			}
		};
		try {
			new ProgressMonitorDialog(this.shell).run(true, false, op);
		} catch (InvocationTargetException e) {
			Activator.logError("InvocationTargetException", e);
		} catch (InterruptedException e) {
			Activator.logError("InterruptedException", e);
		}

	}

	public void removeChangeListener(IExtensionListViewer viewer) {
		changeListeners.remove(viewer);
	}

	public void addChangeListener(IExtensionListViewer viewer) {
		changeListeners.add(viewer);
	}	

}
