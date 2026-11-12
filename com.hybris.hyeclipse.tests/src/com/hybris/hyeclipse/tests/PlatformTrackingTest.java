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
package com.hybris.hyeclipse.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.platform.Platform;

public class PlatformTrackingTest {

	@Test
	public void detectsNatureAddedAfterCreationAndTracksCloseAndReopen() throws Exception {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
		Assert.assertFalse("Test requires an isolated workspace", project.exists());
		Assert.assertFalse(Platform.holder.isPlatformAvailable());
		final NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			project.create(monitor);
			project.open(monitor);
			Assert.assertFalse(Platform.holder.isPlatformAvailable());
			final IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID });
			project.setDescription(description, monitor);
			Assert.assertTrue(Platform.holder.isPlatformAvailable());
			Assert.assertEquals(project, Platform.holder.getCurrent().getPlatformHome());
			project.close(monitor);
			Assert.assertFalse(Platform.holder.isPlatformAvailable());
			project.open(monitor);
			Assert.assertTrue(Platform.holder.isPlatformAvailable());
		} finally {
			if (project.exists()) {
				project.delete(true, true, monitor);
			}
		}
		Assert.assertFalse(Platform.holder.isPlatformAvailable());
	}
}
