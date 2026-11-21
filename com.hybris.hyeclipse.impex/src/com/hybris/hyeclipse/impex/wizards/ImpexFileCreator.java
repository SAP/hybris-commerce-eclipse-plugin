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
package com.hybris.hyeclipse.impex.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/** Workspace-only file creation shared by the wizard and its integration tests. */
public final class ImpexFileCreator {

	public static final String PLUGIN_ID = "com.hybris.hyeclipse.impex"; //$NON-NLS-1$
	public static final String EDITOR_ID = PLUGIN_ID + ".editor"; //$NON-NLS-1$
	public static final String DEFAULT_FILE_NAME = "test.impex"; //$NON-NLS-1$

	private ImpexFileCreator() {
	}

	/**
	 * Validates a single file name without modifying workspace resources.
	 *
	 * @param fileName the requested name, including its extension
	 * @return a status suitable for display by the wizard
	 */
	public static IStatus validateFileName(final String fileName) {
		if (fileName == null || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
			return new Status(IStatus.ERROR, PLUGIN_ID, Messages.invalidFileName);
		}
		final IStatus status = ResourcesPlugin.getWorkspace().validateName(fileName, IResource.FILE);
		if (!status.isOK()) {
			return status;
		}
		final String extension = new Path(fileName).getFileExtension();
		if (!"impex".equalsIgnoreCase(extension) && !"csv".equalsIgnoreCase(extension)) { //$NON-NLS-1$ //$NON-NLS-2$
			return new Status(IStatus.ERROR, PLUGIN_ID, Messages.invalidExtension);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Creates a UTF-8 file under a workspace scheduling rule. Existing files are never replaced.
	 *
	 * @param container an existing, accessible project or folder
	 * @param fileName a validated .impex or .csv name
	 * @param monitor progress and cancellation monitor
	 * @return the newly created file
	 * @throws CoreException if validation, file creation, or charset assignment fails
	 */
	public static IFile create(final IContainer container, final String fileName,
			final IProgressMonitor monitor) throws CoreException {
		final IStatus status = validateFileName(fileName);
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		if (container == null || container.getType() == IResource.ROOT) {
			throw error("An existing project or folder is required"); //$NON-NLS-1$
		}
		final IFile file = container.getFile(new Path(fileName));
		// setCharset updates project-scoped metadata, requiring more than a folder creation rule.
		container.getWorkspace().run(progress -> {
			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			}
			final ResourceAttributes attributes = container.getResourceAttributes();
			if (!container.isAccessible() || (attributes != null && attributes.isReadOnly())) {
				throw error("The destination container is not accessible or is read-only"); //$NON-NLS-1$
			}
			if (file.exists()) {
				throw error("The destination file already exists"); //$NON-NLS-1$
			}
			final String delimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME,
					Platform.PREF_LINE_SEPARATOR, System.lineSeparator(), new IScopeContext[] {
						new ProjectScope(container.getProject()), InstanceScope.INSTANCE, DefaultScope.INSTANCE});
			final byte[] contents = (Messages.starterComment + delimiter).getBytes(StandardCharsets.UTF_8);
			try (final ByteArrayInputStream stream = new ByteArrayInputStream(contents)) {
				// force=false also protects a local file not yet known to the workspace.
				file.create(stream, false, progress);
			} catch (final IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Could not close file contents", e)); //$NON-NLS-1$
			}
			file.setCharset(StandardCharsets.UTF_8.name(), progress);
		}, container.getWorkspace().getRuleFactory().modifyRule(container.getProject()), IResource.NONE, monitor);
		return file;
	}

	private static CoreException error(final String message) {
		return new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, message));
	}
}
