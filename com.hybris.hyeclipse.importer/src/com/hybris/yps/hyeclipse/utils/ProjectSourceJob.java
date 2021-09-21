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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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

/**
 * Utility class around source attachment.
 * 
 * @author mheuer
 * @author pawel wolanski
 *
 */
public class ProjectSourceJob extends Job
{
	private final File sourceArchive;
	private final boolean isAttach;
	
	/**
	 * Instantiate Job with required reference to source archive and flag if that should be attached or not
	 * 
	 * @param sourceArchive - archive to use when attaching, optional for removal
	 */
	public ProjectSourceJob(@NonNull final File sourceArchive) 
	{		
		super("project-source-" + sourceArchive.getName());
		this.sourceArchive = sourceArchive;
		this.isAttach = true;
	}
	
	/**
	 * Instantiate Job with empty source archive and flag to remove links to any source archive.
	 * 
	 */
	public ProjectSourceJob() 
	{		
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
	private void processProject(IProgressMonitor monitor, boolean attach, IProject genProject, File sourceArchive) 
	{
		if (!isJavaProject(genProject)) {
			Activator.log("Skipping non-java project: " + genProject.getName());
		}
		
		// Looks a bit funny, but we are not actually creating a new project.
		IJavaProject project = JavaCore.create(genProject);
		
		try
		{
			IClasspathEntry[] classpathEntries = project.getRawClasspath();
			boolean dirty = false;
			for (int i=0; i < classpathEntries.length; i++) 
			{
				IClasspathEntry entry = classpathEntries[i];
				// Logic:
				// (1) Filter for kind=lib: entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
				// (2) Filter for path ending in server.jar
				// (2.bis) Hybris JAR are : ybootstrap.jar or ytomcat.jar or yant.jar
				boolean isLibrary = entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY;
				String entryFileName = entry.getPath().lastSegment();
				boolean isServerJar = entryFileName.endsWith( "server.jar" );
				boolean isHybrisJar = entryFileName.equals( "ybootstrap.jar" ) || entryFileName.equals( "ytomcat.jar" ) || entryFileName.equals( "yant.jar" );
				boolean isJarWithSource = isServerJar || isHybrisJar;
				if( isJarWithSource && isLibrary )
				{
					IClasspathEntry newEntry = null;
					if (attach) 
					{
						if (sourceArchive==null) {
							throw new IllegalArgumentException("source archive is null");
						}
						Activator.log( "Attaching source: " + sourceArchive + " in: " + genProject.getName() );
						newEntry = JavaCore.newLibraryEntry( entry.getPath(), Path.fromOSString( sourceArchive.getAbsolutePath() ),
							entry.getSourceAttachmentRootPath(), entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported() );
					} 
					else
					{
						Activator.log( "Detaching source from: " + genProject.getName() );
						newEntry = JavaCore.newLibraryEntry( entry.getPath(), null, null, entry.getAccessRules(), entry.getExtraAttributes(), entry.isExported() );
					}
					classpathEntries[i] = newEntry;
					dirty = true;
				}
			}
			
			// And save it all back if we modified something
			if (dirty) 
			{
				project.setRawClasspath( classpathEntries, monitor );
			}
			
		}
		catch( JavaModelException e )
		{
			Activator.log("Error getting classpath entries for project (" + genProject.getName() + "): " + e.getMessage()+". Skipping this project.");
		}
	}

	/**
	 * Helper to provide a clean check if a project is a Java project
	 * 
	 * @param project
	 * @return true if it has a Java nature
	 */
	private boolean isJavaProject( IProject project ) 
	{
		try
		{
			return project.hasNature(JavaCore.NATURE_ID);
		}
		catch( CoreException e )
		{
			// Don't care to report, just motor on
			Activator.logError("CoreException", e);
		}
		return false;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IProject> projects = Arrays.asList( ResourcesPlugin.getWorkspace().getRoot().getProjects() );
		int progress = 0;
		for( IProject project: projects )
		{
			if( FixProjectsUtils.isAHybrisExtension( project ) )
			{
				processProject( monitor, isAttach, project, sourceArchive );
			}
			progress++;
			monitor.worked( progress );
		}
		return Status.OK_STATUS;
	}
}
