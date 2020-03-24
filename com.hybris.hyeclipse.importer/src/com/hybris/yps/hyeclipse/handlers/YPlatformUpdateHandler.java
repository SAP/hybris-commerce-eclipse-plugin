package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.BuildUtils;

public class YPlatformUpdateHandler extends AbstractHandler {

	private static final String CFG_NAME = "yplatform_update";
	private static final String PLATFORM_NAME = "platform";

	@Override
	public Object execute(ExecutionEvent event) {

		Job job = new Job("[y] Update") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IProject platform = ResourcesPlugin.getWorkspace().getRoot().getProject(PLATFORM_NAME);
				try {
					BuildUtils.refreshAndBuild(monitor, CFG_NAME, platform);
					monitor.done();
					return Status.OK_STATUS;
				} catch (Exception e) {
					Activator.logError("Failed to update", e);
					throw new IllegalStateException("Failed to update with the platform", e);
				}

			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}
}
