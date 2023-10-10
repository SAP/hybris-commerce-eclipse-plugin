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
package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.BuildUtils;

public class YPlatformRefreshAndBuildHandler extends AbstractHandler {
	private static final String CFG_NAME = "yplatform_build";

	@Override
	public Object execute(ExecutionEvent event) {

		Job job = new Job(" Refresh and Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IProject platform = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
				try {
					BuildUtils.refreshAndBuild(monitor, CFG_NAME, platform);
					monitor.done();
					return Status.OK_STATUS;
				} catch (Exception e) {
					Activator.logError("Failed to refresh and build", e);
					throw new IllegalStateException("Failed to refresh and build the platform, see the workspace logs for more details", e);
				}

			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

}
