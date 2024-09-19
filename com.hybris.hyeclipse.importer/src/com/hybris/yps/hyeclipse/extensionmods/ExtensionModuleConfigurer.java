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
package com.hybris.yps.hyeclipse.extensionmods;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.ExtensionHolder;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;

public class ExtensionModuleConfigurer {

	private Set<ExtensionHolder> allPlatformExtensions;
	private Set<IExtensionListViewer> changeListeners = new HashSet<>();
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

					if (com.hybris.hyeclipse.commons.Activator.resetPlatformBootstrapBundle() != null) {
						monitor.worked(1);
						allPlatformExtensions = FixProjectsUtils
								.getAllExtensionsForPlatform();
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
			Thread.currentThread().interrupt();
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
			Thread.currentThread().interrupt();
		}

	}

	public void removeChangeListener(IExtensionListViewer viewer) {
		changeListeners.remove(viewer);
	}

	public void addChangeListener(IExtensionListViewer viewer) {
		changeListeners.add(viewer);
	}	

}
