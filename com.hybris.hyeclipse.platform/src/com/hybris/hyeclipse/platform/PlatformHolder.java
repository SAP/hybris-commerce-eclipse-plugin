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
package com.hybris.hyeclipse.platform;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class PlatformHolder {
	
	private final ILog log;
	
	private final ClasspathChagedListener classpathChagedListener = new ClasspathChagedListener(this);
	private final WorkspaceChangedListener workspaceChangeListener = new WorkspaceChangedListener(this);
	
	private volatile Platform currentPlatform;
	
	PlatformHolder(ILog log) {
		this.log = log;
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (final IProject project : workspace.getRoot().getProjects()) {
			if (Platform.isPlatformProject(project)) {
				platformProjectAdded(project);
			}
		}
		
		addResourceChangeListenerToWorkspace();
	}
	
	private void addResourceChangeListenerToWorkspace() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(workspaceChangeListener, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_CLOSE);
	}

	public Platform getCurrent() {
		return currentPlatform;
	}
	
	public boolean isPlatformAvailable() {
		return currentPlatform != null;
	}
	
	private void updatePlatform(final IJavaProject platformJavaProject) {
		try {
			currentPlatform = new Platform(platformJavaProject);
		}
		catch (CoreException e) {
			log.log(new Status(Status.ERROR, log.getBundle().getSymbolicName(), "Failed to update platform", e));
		}
	}
	
	private void platformProjectAdded(final IProject project) {
		assert !isPlatformAvailable();
		assert Platform.isPlatformProject(project);
		
		final IJavaProject platformJavaProject = JavaCore.create(project);
		
		updatePlatform(platformJavaProject);
		JavaCore.addElementChangedListener(classpathChagedListener, ElementChangedEvent.POST_CHANGE);
	}
	
	void clear() {
		JavaCore.removeElementChangedListener(classpathChagedListener);
		currentPlatform = null;
		addResourceChangeListenerToWorkspace();
	}
	
	void classPathChanged() {
		assert isPlatformAvailable();
		
		updatePlatform(currentPlatform.getPlatformJavaProject());
	}

	boolean isCurrentPlatformProject(IJavaProject javaProject) {
		return isPlatformAvailable() && currentPlatform.getPlatformJavaProject() == javaProject;
	}
	
	void projectAdded(final IProject project) {
		if (isPlatformAvailable() && Platform.isPlatformProject(project)) {
			log.log(new Status(Status.ERROR, log.getBundle().getSymbolicName(), "Platform project added while platform already available. There can only be ONE."));
		}
	}

	void workspaceChanged(IResourceChangeEvent event) {
		try {
			if (event.getType() == IResourceChangeEvent.PRE_DELETE || event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				if (Platform.isPlatformProject((IProject)event.getResource())) {
					clear();
				}
			}
			else if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				event.getDelta().accept(new IResourceDeltaVisitor() {

					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						if (delta.getKind() == IResourceDelta.ADDED
								&& delta.getResource().getType() == IResource.PROJECT) {
							projectAdded((IProject) delta.getResource());
						}
						return delta.getResource().getType() == IResource.ROOT;
					}

				});
			}
		} catch (CoreException e) {
			log.log(new Status(Status.ERROR, log.getBundle().getSymbolicName(), "Error when handling IResourceChangedEvent", e));
		}
	}

}
