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
package com.hybris.hyeclipse.commons.typesystem;

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.hybris.hyeclipse.commons.Activator;
import com.hybris.hyeclipse.commons.Constants;

/** Lifecycle owner for non-blocking IDE lookups and coalesced post-build indexing. */
public final class TypeSystemIndex implements IResourceChangeListener {
	private static final long REFRESH_DELAY_MILLIS = 300;
	private final IWorkspace workspace;
	private final TypeSystemRepository repository;
	private final Job refreshJob;
	private boolean active;
	private String initializedHome;
	private boolean refreshRequested;

	/**
	 * @param workspace owning workspace
	 * @param repository persistent snapshot store
	 */
	public TypeSystemIndex(final IWorkspace workspace, final TypeSystemRepository repository) {
		this.workspace = workspace;
		this.repository = repository;
		refreshJob = new Job(TypeSystemMessages.index_job_title) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				return update(monitor);
			}
		};
		refreshJob.setSystem(true);
		refreshJob.setRule(workspace.getRoot());
	}

	/** Registers the listener and restores or creates the initial snapshot in a background Job. */
	public synchronized void start() {
		if (!active) {
			active = true;
			workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD);
			refreshJob.schedule();
		}
	}

	/** Unregisters the workspace listener and cancels pending work. */
	public synchronized void stop() {
		active = false;
		workspace.removeResourceChangeListener(this);
		refreshJob.cancel();
	}

	/** @return latest snapshot without parsing, disk I/O or blocking the UI */
	public TypeSystemSnapshot getSnapshot() {
		final IProject platform = platform();
		return platform.isOpen() && platform.getLocation() != null
				? repository.peek(platform.getLocation().toFile()) : TypeSystemSnapshot.empty();
	}

	/**
	 * Waits for pending indexing from a background operation, never from the UI thread.
	 * @param monitor cancellation monitor
	 * @throws InterruptedException if the operation is interrupted
	 */
	public void awaitPendingRefresh(final IProgressMonitor monitor) throws InterruptedException {
		refreshJob.join(0, monitor);
	}

	/** Schedules a complete replacement scan after import or a build. */
	public synchronized void requestRefresh() {
		if (active) {
			refreshRequested = true;
			refreshJob.schedule(REFRESH_DELAY_MILLIS);
		}
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_BUILD) {
			requestRefresh();
		}
	}

	private IProject platform() {
		return workspace.getRoot().getProject(Constants.PLATFROM);
	}

	private IStatus update(final IProgressMonitor monitor) {
		final boolean scan;
		synchronized (this) {
			if (!active) {
				return Status.CANCEL_STATUS;
			}
			scan = refreshRequested;
			refreshRequested = false;
		}
		final IProject platform = platform();
		if (!platform.isOpen() || platform.getLocation() == null) {
			return Status.OK_STATUS;
		}
		try {
			final String home = platform.getLocation().toOSString();
			if (!home.equals(initializedHome) && !scan) {
				try {
					// Publish saved data first, then reconcile with current sources without blocking consumers.
					repository.load(platform.getLocation().toFile());
					initializedHome = home;
				} catch (IOException e) {
					Activator.logError("Could not restore type index; rebuilding from generated sources", e);
				}
			}
			final IFolder generated =
					platform.getFolder(TypeSystemConstants.GENERATED_SOURCE_DIRECTORY);
			if (!generated.exists() && (generated.getLocation() == null
					|| !generated.getLocation().toFile().isDirectory())) {
				return Status.OK_STATUS;
			}
			final TypeSystemSnapshot snapshot = new GeneratedModelScanner().scan(platform, monitor);
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			synchronized (this) {
				if (!active) {
					return Status.CANCEL_STATUS;
				}
				repository.save(platform.getLocation().toFile(), snapshot);
				initializedHome = home;
			}
			return Status.OK_STATUS;
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (CoreException | IOException | RuntimeException e) {
			Activator.logError("Could not refresh generated type index; retaining the previous snapshot", e);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, TypeSystemMessages.index_error, e);
		} finally {
			monitor.done();
		}
	}
}
