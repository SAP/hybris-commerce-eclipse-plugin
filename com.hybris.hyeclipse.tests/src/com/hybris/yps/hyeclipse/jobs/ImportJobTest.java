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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hybris.yps.hyeclipse.wizards.ImportJobData;

public class ImportJobTest {

	private boolean originalAutoBuild;

	@Before
	public void saveWorkspaceSettings() {
		originalAutoBuild = ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
	}

	@After
	public void restoreWorkspaceSettings() throws CoreException {
		Thread.interrupted();
		setAutoBuild(originalAutoBuild);
	}

	@Test
	public void successPreservesDisabledAutoBuildAndOrdersFollowUp() throws Exception {
		setAutoBuild(false);
		final TestImportJob job = new TestImportJob();
		Assert.assertTrue(job.run(new NullProgressMonitor()).isOK());
		Assert.assertFalse(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
		Assert.assertEquals(Arrays.asList("dependencies", "import", "save", "completed"), job.events);
	}

	@Test
	public void successRestoresEnabledAutoBuildBeforeFollowUp() throws Exception {
		setAutoBuild(true);
		final TestImportJob job = new TestImportJob();
		Assert.assertTrue(job.run(new NullProgressMonitor()).isOK());
		Assert.assertTrue(job.autoBuildAtCompletion);
	}

	@Test
	public void dependencyFailurePreventsImportAndFollowUp() throws Exception {
		setAutoBuild(true);
		final TestImportJob job = new TestImportJob();
		job.dependencyFailure = new IOException("Ant failed");
		Assert.assertEquals(IStatus.ERROR, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertEquals(Arrays.asList("dependencies"), job.events);
		Assert.assertTrue(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
	}

	@Test
	public void importFailureRestoresAutoBuildAndPreventsFollowUp() throws Exception {
		setAutoBuild(true);
		final TestImportJob job = new TestImportJob();
		job.importFailure = new CoreException(new Status(IStatus.ERROR, "test", "Import failed"));
		Assert.assertEquals(IStatus.ERROR, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertEquals(Arrays.asList("dependencies", "import"), job.events);
		Assert.assertTrue(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
	}

	@Test
	public void incompleteAntOutputPreventsImportAndPersistence() throws Exception {
		final TestImportJob job = new TestImportJob();
		job.incompleteOutput = true;
		Assert.assertEquals(IStatus.ERROR, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertEquals(Arrays.asList("dependencies"), job.events);
	}

	@Test
	public void cancellationAfterImportPreventsPersistenceAndFollowUp() throws Exception {
		final TestImportJob job = new TestImportJob();
		job.cancelDuringImport = true;
		Assert.assertEquals(IStatus.CANCEL, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertEquals(Arrays.asList("dependencies", "import"), job.events);
	}

	@Test
	public void persistenceFailurePreventsFollowUp() throws Exception {
		final TestImportJob job = new TestImportJob();
		job.saveFailure = new CoreException(new Status(IStatus.ERROR, "test", "Save failed"));
		Assert.assertEquals(IStatus.ERROR, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertEquals(Arrays.asList("dependencies", "import", "save"), job.events);
	}

	@Test
	public void cancellationRestoresAutoBuildWithoutStartingWork() throws Exception {
		setAutoBuild(true);
		final TestImportJob job = new TestImportJob();
		final NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		Assert.assertEquals(IStatus.CANCEL, job.run(monitor).getSeverity());
		Assert.assertTrue(job.events.isEmpty());
		Assert.assertTrue(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
	}

	@Test
	public void interruptionPreservesInterruptFlagAndRestoresAutoBuild() throws Exception {
		setAutoBuild(true);
		final TestImportJob job = new TestImportJob();
		job.interrupt = true;
		Assert.assertEquals(IStatus.CANCEL, job.run(new NullProgressMonitor()).getSeverity());
		Assert.assertTrue(Thread.currentThread().isInterrupted());
		Assert.assertTrue(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
		Assert.assertEquals(Arrays.asList("dependencies"), job.events);
	}

	private void setAutoBuild(final boolean enabled) throws CoreException {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(enabled);
		workspace.setDescription(description);
	}

	private static class TestImportJob extends ImportJob {
		private final List<String> events = new ArrayList<>();
		private IOException dependencyFailure;
		private CoreException importFailure;
		private CoreException saveFailure;
		private boolean interrupt;
		private boolean autoBuildAtCompletion;
		private boolean incompleteOutput;
		private boolean cancelDuringImport;

		TestImportJob() {
			super(new ImportJobData(false, true, true, true, true, true, new File("platform")), new File("sources.zip"));
		}

		@Override
		protected Collection<String> updateDependencies(final IProgressMonitor monitor)
				throws IOException, InterruptedException {
			events.add("dependencies");
			Assert.assertFalse(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
			if (interrupt) {
				throw new InterruptedException();
			}
			if (dependencyFailure != null) {
				throw dependencyFailure;
			}
			if (incompleteOutput) {
				return new ArrayList<>();
			}
			return Arrays.asList("[echo] Platform home: " + new File("platform").getAbsolutePath(),
					"[echo] hybris Platform version: 2211", "[echo] core 2211 [pci]");
		}

		@Override
		protected void importProjects(final IProgressMonitor monitor) throws CoreException {
			events.add("import");
			if (importFailure != null) {
				throw importFailure;
			}
			if (cancelDuringImport) {
				monitor.setCanceled(true);
			}
		}

		@Override
		protected void saveSystemConfig() throws CoreException {
			events.add("save");
			Assert.assertFalse(ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding());
			if (saveFailure != null) {
				throw saveFailure;
			}
		}

		@Override
		protected void importCompleted() {
			events.add("completed");
			autoBuildAtCompletion = ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		}
	}
}
