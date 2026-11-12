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
package com.hybris.yps.hyeclipse.validation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.commons.validation.ValidationResult;
import com.hybris.hyeclipse.commons.validation.ValidationResult.State;
import com.hybris.hyeclipse.commons.validation.ValidationXml;
import com.hybris.hyeclipse.tsv.builder.HybrisNature;
import com.hybris.hyeclipse.tsv.builder.StandardTsvBuilder;
import com.hybris.hyeclipse.tsv.model.TSVResults;

public class NativeTsvIntegrationTest {
	@Test
	public void serializesAndReadsReportsWithoutJaxbIncludingSkippedChecks() throws Exception {
		final String report = ValidationXml.report(Arrays.asList(
				new ValidationResult(new File("some & file-items.xml"), 7, "rule", "H", State.FAIL, "Type.field", "Bad <välue>"),
				new ValidationResult(new File("some & file-items.xml"), 8, "skip", "M", State.SKIPPED, "Type", "Missing schema"),
				new ValidationResult(new File("some & file-items.xml"), 9, "pass", "L", State.PASS, "Type", "Passed")));
		final TSVResults results = TSVResults.read(new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8)));
		Assert.assertEquals(2, results.getResults().size());
		Assert.assertTrue(results.getResults().stream().anyMatch(result -> "SKIPPED".equals(result.getState())));
		Assert.assertTrue(results.getResults().stream().anyMatch(result -> "Bad <välue>".equals(result.getDescription())));
		Assert.assertTrue(results.getResults().stream().anyMatch(result -> result.getLineNumber() == 7));
	}

	@Test
	public void nativeBuilderReportsMalformedXmlAndClearsMarkersAfterCorrection() throws Exception {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject("native-tsv-test");
		Assert.assertFalse(project.exists());
		final NullProgressMonitor monitor = new NullProgressMonitor();
		final boolean autoBuild = workspace.getDescription().isAutoBuilding();
		final IWorkspaceDescription settings = workspace.getDescription();
		settings.setAutoBuilding(false);
		workspace.setDescription(settings);
		try {
			project.create(monitor);
			project.open(monitor);
			final IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { HybrisNature.NATURE_ID });
			final ICommand builder = description.newCommand();
			builder.setBuilderName(StandardTsvBuilder.BUILDER_ID);
			description.setBuildSpec(new ICommand[] { builder });
			project.setDescription(description, monitor);
			final IFile file = project.getFile("test-items.xml");
			file.create(new ByteArrayInputStream("<items>".getBytes(StandardCharsets.UTF_8)), true, monitor);
			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			final IMarker[] errors = file.findMarkers("com.hybris.hyeclipse.tsv.tsvproblem", false, IResource.DEPTH_ZERO);
			Assert.assertTrue(errors.length > 0);
			Assert.assertEquals(IMarker.SEVERITY_ERROR, errors[0].getAttribute(IMarker.SEVERITY, 0));
			final String valid = "<items><itemtypes><itemtype code=\"Valid\" extends=\"GenericItem\">"
					+ "<deployment table=\"valid\" typecode=\"10001\"/></itemtype></itemtypes></items>";
			file.setContents(new ByteArrayInputStream(valid.getBytes(StandardCharsets.UTF_8)), true, false, monitor);
			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			Assert.assertEquals(0, file.findMarkers("com.hybris.hyeclipse.tsv.tsvproblem", false, IResource.DEPTH_ZERO).length);
		} finally {
			if (project.exists()) {
				project.delete(true, true, monitor);
			}
			final IWorkspaceDescription restored = workspace.getDescription();
			restored.setAutoBuilding(autoBuild);
			workspace.setDescription(restored);
		}
	}
}
