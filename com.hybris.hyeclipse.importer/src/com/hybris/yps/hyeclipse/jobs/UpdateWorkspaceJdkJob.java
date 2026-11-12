/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;

import com.hybris.yps.hyeclipse.utils.ProjectJavaRuntime;
import com.hybris.yps.hyeclipse.wizards.Messages;

/** User-confirmed update; acquires the import lock before the workspace rule to avoid child-Job deadlocks. */
public final class UpdateWorkspaceJdkJob extends Job {
	private final IVMInstall vm;

	/** @param vm JDK confirmed by the user */
	public UpdateWorkspaceJdkJob(final IVMInstall vm) {
		super(Messages.ProjectJavaRuntime_update_job);
		this.vm = vm;
		setUser(true);
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			return ProjectJavaRuntime.updateWorkspace(vm, monitor);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
	}
}
