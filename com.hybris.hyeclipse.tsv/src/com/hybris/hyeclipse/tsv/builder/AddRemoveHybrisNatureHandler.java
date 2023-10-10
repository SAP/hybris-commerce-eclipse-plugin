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
package com.hybris.hyeclipse.tsv.builder;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.tsv.Plugin;

public class AddRemoveHybrisNatureHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO Auto-generated method stub
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		//
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it
					.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}
				if (project != null) {
					toggleNature(project);
				}
			}
		}

		return null;
	}

	/**
	 * Toggles sample nature on a project asynchronously
	 *
	 * @param project
	 *            to have sample nature added or removed
	 */
	private void toggleNature(final IProject project) {
		new Job("Adding SAP Nature") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProjectDescription description = project.getDescription();
					String[] natures = description.getNatureIds();

					for (int i = 0; i < natures.length; ++i) {
						if (HybrisNature.NATURE_ID.equals(natures[i])) {
							// Remove the nature
							String[] newNatures = new String[natures.length - 1];
							System.arraycopy(natures, 0, newNatures, 0, i);
							System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
							description.setNatureIds(newNatures);
							project.setDescription(description, monitor);
							return Status.OK_STATUS;
						}
					}

					// Add the nature
					String[] newNatures = new String[natures.length + 1];
					System.arraycopy(natures, 0, newNatures, 0, natures.length);
					newNatures[natures.length] = HybrisNature.NATURE_ID;
					description.setNatureIds(newNatures);
					project.setDescription(description, monitor);
					return Status.OK_STATUS;
				}
				catch (CoreException e) {
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Failed to add SAP nature", e);
				}
			}
		}.schedule();
	}

}
