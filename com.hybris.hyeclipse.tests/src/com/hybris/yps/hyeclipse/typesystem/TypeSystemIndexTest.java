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
package com.hybris.yps.hyeclipse.typesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.hyeclipse.commons.typesystem.TypeSystemIndex;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemRepository;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;

public class TypeSystemIndexTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();
	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	private IProject platform;
	private TypeSystemIndex index;
	private boolean autoBuild;
	private boolean created;

	@Before
	public void setUp() throws Exception {
		platform = workspace.getRoot().getProject("platform");
		Assume.assumeFalse(platform.exists());
		autoBuild = workspace.getDescription().isAutoBuilding();
		setAutoBuild(false);
		platform.create(monitor);
		created = true;
		platform.open(monitor);
		platform.getFolder("bootstrap").create(true, true, monitor);
		platform.getFolder("bootstrap/gensrc").create(true, true, monitor);
	}

	@After
	public void tearDown() throws Exception {
		if (index != null) {
			index.stop();
			index.awaitPendingRefresh(monitor);
		}
		if (created) {
			platform.delete(true, true, monitor);
			setAutoBuild(autoBuild);
		}
	}

	@Test
	public void refreshesAfterFullAndIncrementalBuildAndRemovesDeletedTypes() throws Exception {
		write("DealerModel.java", model("Dealer", "dealer"));
		final File state = temporary.newFolder("state");
		final TypeSystemRepository repository = new TypeSystemRepository(state);
		index = new TypeSystemIndex(workspace, repository);
		index.start();
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Dealer"), index.getSnapshot().getAllTypeNames());
		final TypeSystemSnapshot old = index.getSnapshot();
		write("DealerModel.java", model("Dealer", "updated"));
		platform.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("updated"), index.getSnapshot().getAllAttributeNames("Dealer"));
		Assert.assertEquals(Arrays.asList("dealer"), old.getAllAttributeNames("Dealer"));
		write("OtherModel.java", model("Other", "other"));
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Dealer", "Other"), index.getSnapshot().getAllTypeNames());
		platform.getFile("bootstrap/gensrc/DealerModel.java").delete(true, monitor);
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Other"), index.getSnapshot().getAllTypeNames());
		Assert.assertEquals(Arrays.asList("Other"), new TypeSystemRepository(state)
				.load(platform.getLocation().toFile()).get().getAllTypeNames());
	}

	@Test
	public void importRefreshFindsSourcesCreatedOutsideEclipse() throws Exception {
		index = new TypeSystemIndex(workspace, new TypeSystemRepository(temporary.newFolder("state")));
		index.start();
		index.awaitPendingRefresh(monitor);
		Files.writeString(platform.getLocation().toFile().toPath().resolve("bootstrap/gensrc/DealerModel.java"),
				model("Dealer", "dealer"));
		index.requestRefresh();
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Dealer"), index.getSnapshot().getAllTypeNames());
	}

	@Test
	public void refreshesAfterAutomaticBuilds() throws Exception {
		write("DealerModel.java", model("Dealer", "dealer"));
		index = new TypeSystemIndex(workspace, new TypeSystemRepository(temporary.newFolder("state")));
		index.start();
		index.awaitPendingRefresh(monitor);
		setAutoBuild(true);
		write("DealerModel.java", model("Dealer", "automaticallyUpdated"));
		Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, monitor);
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("automaticallyUpdated"), index.getSnapshot().getAllAttributeNames("Dealer"));
	}

	@Test
	public void restoresBeforeFirstBuildAndRetainsGoodSnapshotWhenSourceIsBroken() throws Exception {
		write("DealerModel.java", model("Dealer", "dealer"));
		final File state = temporary.newFolder("state");
		index = new TypeSystemIndex(workspace, new TypeSystemRepository(state));
		index.start();
		index.awaitPendingRefresh(monitor);
		index.stop();
		index.awaitPendingRefresh(monitor);
		write("DealerModel.java", "public class Broken {");
		index = new TypeSystemIndex(workspace, new TypeSystemRepository(state));
		index.start();
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Dealer"), index.getSnapshot().getAllTypeNames());
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		index.awaitPendingRefresh(monitor);
		Assert.assertEquals(Arrays.asList("Dealer"), index.getSnapshot().getAllTypeNames());
		platform.close(monitor);
		Assert.assertTrue(index.getSnapshot().getTypes().isEmpty());
	}

	private static String model(final String code, final String qualifier) {
		return GeneratedModelScannerTest.source(code + "Model", "ItemModel",
				"public static final String _TYPECODE=\"" + code + "\"; "
				+ "@Accessor(qualifier=\"" + qualifier + "\") public abstract String getAttribute();");
	}

	private void write(final String name, final String source) throws CoreException {
		final IFile file = platform.getFile("bootstrap/gensrc/" + name);
		final ByteArrayInputStream input = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
		if (file.exists()) {
			file.setContents(input, true, false, monitor);
		} else {
			file.create(input, true, monitor);
		}
	}

	private void setAutoBuild(final boolean enabled) throws CoreException {
		final IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(enabled);
		workspace.setDescription(description);
	}
}
