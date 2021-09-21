/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.wizards.Messages;

/**
 * Utility class around source attachment.
 * 
 * @author mheuer
 * @author pawel wolanski
 *
 */
public class ProjectSourceJob extends Job {
	private final File sourceArchive;
	private final boolean isAttach;

	/**
	 * Instantiate Job with required reference to source archive and flag if that
	 * should be attached or not
	 * 
	 * @param sourceArchive - archive to use when attaching, optional for removal
	 */
	public ProjectSourceJob(@NonNull final File sourceArchive) {
		super("project-source-" + sourceArchive.getName());
		this.sourceArchive = sourceArchive;
		this.isAttach = true;
	}

	/**
	 * Instantiate Job with empty source archive and flag to remove links to any
	 * source archive.
	 * 
	 */
	public ProjectSourceJob() {
		super("project-source-code-unlink");
		this.sourceArchive = null;
		this.isAttach = false;
	}

	/**
	 * Worker method to attach the given source archive to the project.
	 * 
	 * @param monitor
	 * @param genProject
	 * @param attach
	 * @param sourceArchive
	 */
	private void processProject(IProgressMonitor monitor, boolean attach, IProject genProject, File sourceArchive) {
		IJavaProject project = JavaCore.create(genProject);
		String projectName = genProject.getName();
		try {
			List<IClasspathEntry> hybrisLibs = Arrays.stream(project.getRawClasspath()).filter(getHybrisLibs())
					.collect(Collectors.toList());
			if (!hybrisLibs.isEmpty()) {
				List<IClasspathEntry> newLibs;
				if (attach) {
					newLibs = hybrisLibs.stream()
							.map(e -> JavaCore.newLibraryEntry(e.getPath(),
									Path.fromOSString(sourceArchive.getAbsolutePath()),
									e.getSourceAttachmentRootPath(), e.getAccessRules(),
									e.getExtraAttributes(), e.isExported()))
							.collect(Collectors.toList());
				} else {
					newLibs = hybrisLibs
							.stream().map(e -> JavaCore.newLibraryEntry(e.getPath(), null, null,
									e.getAccessRules(), e.getExtraAttributes(), e.isExported()))
							.collect(Collectors.toList());
				}
				List<IClasspathEntry> otherLibs = Arrays.stream(project.getRawClasspath())
						.filter(Predicate.not(getHybrisLibs())).collect(Collectors.toList());
				otherLibs.addAll(newLibs);

				project.setRawClasspath(otherLibs.stream().toArray(IClasspathEntry[]::new), monitor);

			}
		} catch (JavaModelException e) {
			Activator.log(String.format(Messages.ProjectSourceJob_classpath_fetch_error, projectName, e.getMessage()));
		}
	}

	private Predicate<? super IClasspathEntry> getHybrisLibs() {
		// Logic:
		// (1) Filter for kind=lib: entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
		// (2) Filter for path ending in server.jar
		// (2.bis) Hybris JAR are : ybootstrap.jar or ytomcat.jar or yant.jar
		return p -> p.getContentKind() == IClasspathEntry.CPE_LIBRARY && (StringUtils
				.endsWithAny(p.getPath().lastSegment(), "server.jar", "ybootstrap.jar", "ytomcat.jar", "yant.jar"));
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		int progress = 0;
		for (IProject project : projects) {
			if (FixProjectsUtils.isAHybrisExtension(project)) {
				processProject(monitor, isAttach, project, sourceArchive);
			}
			progress++;
			monitor.worked(progress);
		}
		return Status.OK_STATUS;
	}
}
