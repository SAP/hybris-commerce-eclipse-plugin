/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.tests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hybris.hyeclipse.impex.wizards.ImpexFileCreator;

public class ImpexFileCreatorTest {

	private IProject project;

	@Before
	public void createProject() throws Exception {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("impex-test-" + UUID.randomUUID());
		project.create(null);
		project.open(null);
	}

	@After
	public void deleteProject() throws Exception {
		if (project != null && project.exists()) {
			project.delete(true, true, null);
		}
	}

	@Test
	public void validatesNamesAndExtensions() {
		for (final String valid : new String[] {"test.impex", "test.IMPEX", "test.csv", "test.CSV", "é.impex"}) {
			Assert.assertTrue(valid, ImpexFileCreator.validateFileName(valid).isOK());
		}
		for (final String invalid : new String[] {null, "", "test", "test.txt", "../test.impex", "/test.impex",
				"folder/test.impex", "folder\\test.impex"}) {
			Assert.assertFalse(String.valueOf(invalid), ImpexFileCreator.validateFileName(invalid).isOK());
		}
	}

	@Test
	public void createsUtf8FileWithProjectLineDelimiter() throws Exception {
		project.setDefaultCharset("UTF-16", null);
		new ProjectScope(project).getNode(Platform.PI_RUNTIME).put(Platform.PREF_LINE_SEPARATOR, "\r\n");
		final IFile file = ImpexFileCreator.create(project, "é.impex", new NullProgressMonitor());
		Assert.assertTrue(file.exists());
		Assert.assertEquals("UTF-8", file.getCharset(false));
		final String contents = read(file);
		Assert.assertTrue(contents.startsWith("# "));
		Assert.assertTrue(contents.endsWith("\r\n"));
		Assert.assertEquals(1, contents.chars().filter(c -> c == '\n').count());
		Assert.assertFalse(contents.startsWith("\uFEFF"));
	}

	@Test
	public void createsFileInFolderAndAcceptsExplicitCsv() throws Exception {
		final IFolder folder = project.getFolder("resources");
		folder.create(false, true, null);
		new ProjectScope(project).getNode(Platform.PI_RUNTIME).put(Platform.PREF_LINE_SEPARATOR, "\n");
		final IFile file = ImpexFileCreator.create(folder, "sample.csv", new NullProgressMonitor());
		Assert.assertEquals(folder, file.getParent());
		Assert.assertTrue(read(file).endsWith("\n"));
		Assert.assertFalse(read(file).contains("\r"));
	}

	@Test
	public void neverOverwritesExistingFile() throws Exception {
		final IFile file = project.getFile("existing.impex");
		try (final ByteArrayInputStream stream = new ByteArrayInputStream("original".getBytes(StandardCharsets.UTF_8))) {
			file.create(stream, false, null);
		}
		try {
			ImpexFileCreator.create(project, file.getName(), new NullProgressMonitor());
			Assert.fail("Expected duplicate-file rejection");
		} catch (final CoreException expected) {
			Assert.assertTrue(expected.getStatus().matches(IStatus.ERROR));
		}
		Assert.assertEquals("original", read(file));
	}

	@Test
	public void rejectsUntrackedLocalFileWithoutOverwritingIt() throws Exception {
		final IFile file = project.getFile("untracked.impex");
		Files.writeString(file.getLocation().toFile().toPath(), "local original", StandardCharsets.UTF_8);
		Assert.assertFalse(file.exists());
		try {
			ImpexFileCreator.create(project, file.getName(), new NullProgressMonitor());
			Assert.fail("Expected local-file rejection");
		} catch (final CoreException expected) {
			Assert.assertEquals("local original", Files.readString(file.getLocation().toFile().toPath()));
		}
	}

	@Test
	public void cancellationDoesNotCreateFile() throws Exception {
		final NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		try {
			ImpexFileCreator.create(project, "cancelled.impex", monitor);
			Assert.fail("Expected cancellation");
		} catch (final OperationCanceledException expected) {
			Assert.assertFalse(project.getFile("cancelled.impex").exists());
		}
	}

	@Test
	public void rejectsClosedProject() throws Exception {
		project.close(null);
		try {
			ImpexFileCreator.create(project, "closed.impex", new NullProgressMonitor());
			Assert.fail("Expected inaccessible-project rejection");
		} catch (final CoreException expected) {
			Assert.assertFalse(project.getFile("closed.impex").exists());
		}
	}

	@Test
	public void rejectsReadOnlyFolder() throws Exception {
		final IFolder folder = project.getFolder("readonly");
		folder.create(false, true, null);
		final ResourceAttributes original = folder.getResourceAttributes();
		final ResourceAttributes readOnly = folder.getResourceAttributes();
		readOnly.setReadOnly(true);
		folder.setResourceAttributes(readOnly);
		try {
			ImpexFileCreator.create(folder, "readonly.impex", new NullProgressMonitor());
			Assert.fail("Expected read-only-folder rejection");
		} catch (final CoreException expected) {
			Assert.assertFalse(folder.getFile("readonly.impex").exists());
		} finally {
			folder.setResourceAttributes(original);
		}
	}

	@Test
	public void rejectsWorkspaceRootAndMissingFolder() throws Exception {
		try {
			ImpexFileCreator.create(ResourcesPlugin.getWorkspace().getRoot(), "root.impex", new NullProgressMonitor());
			Assert.fail("Expected workspace-root rejection");
		} catch (final CoreException expected) {
			Assert.assertTrue(expected.getStatus().matches(IStatus.ERROR));
		}
		try {
			ImpexFileCreator.create(project.getFolder(new Path("missing")), "missing.impex", new NullProgressMonitor());
			Assert.fail("Expected missing-folder rejection");
		} catch (final CoreException expected) {
			Assert.assertFalse(project.getFolder("missing").exists());
		}
	}

	private String read(final IFile file) throws Exception {
		try (final InputStream stream = file.getContents()) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
