package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.BuildUtils;
import com.hybris.yps.hyeclipse.utils.FixProjectsUtils;

/**
 * Handler for building the single project
 */
public class ExtensionBuildHandler extends AbstractHandler {
	protected static final String CFG_NAME = "yplatform_build";
	private IProject project;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		project = getSelectedExtension(HandlerUtil.getCurrentSelection(event));
		Job job = new Job("[y] Build") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (FixProjectsUtils.isAHybrisExtension(project)) {
						BuildUtils.refreshAndBuild(monitor, CFG_NAME, project);
						monitor.done();
						return Status.OK_STATUS;
					} else {
						return Status.CANCEL_STATUS;
					}
				} catch (Exception e) {
					Activator.logError("Failed to build project", e);
					throw new IllegalStateException("Failed to build project, see workspace logs for details", e);
				}

			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	/**
	 * Returns the {@link IProject} project basing on the current selection
	 * 
	 * @param selection
	 *            current selection
	 * @return selected project
	 */
	private IProject getSelectedExtension(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return null;
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		Object element = structuredSelection.getFirstElement();
		if (element instanceof IProject)
			return (IProject) element;
		if (!(element instanceof IAdaptable))
			return null;
		IAdaptable adaptable = (IAdaptable) element;
		Object adapter = adaptable.getAdapter(IProject.class);
		return (IProject) adapter;
	}
}
