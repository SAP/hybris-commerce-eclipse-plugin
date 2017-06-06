package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.hybris.yps.hyeclipse.Activator;

/**
 * Utility class around source attachment.
 * 
 * @author mheuer
 *
 */
public class ProjectSourceUtil
{
	
	/**
	 * Factory method to return a runner for performing the attachment work.
	 * 
	 * @param sourceArchive - archive to use when attaching, optional for removal
	 * @return runner
	 */
	public static IRunnableWithProgress getRunner(final File sourceArchive) 
	{
		return getRunnerInternal( sourceArchive, true );		
	}

	/**
	 * Factory method to return a runner for performing the detachment work.
	 * 
	 * @return runner
	 */
	public static IRunnableWithProgress getRunner() 
	{
		return getRunnerInternal( null, false );		
	}
	
	
	/**
	 * Return an attachment or detachment runner.
	 * 
	 * @param sourceArchive
	 * @param isAttach
	 * @return runner
	 */
	private static IRunnableWithProgress getRunnerInternal( final File sourceArchive, final boolean isAttach )
	{
		IRunnableWithProgress runner = new IRunnableWithProgress()
		{
			@Override
			public void run( IProgressMonitor monitor ) throws InvocationTargetException
			{
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
			}
		};
		
		return runner;
	}

	/**
	 * Worker method to attach the given source archive to the project.
	 * 
	 * @param monitor 
	 * @param genProject
	 * @param attach 
	 * @param sourceArchive
	 */
	private static void processProject(IProgressMonitor monitor, boolean attach, IProject genProject, File sourceArchive) 
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
				//Activator.log("Processing: "+entry.getPath()+", kind="+entry.getEntryKind()+", src=" + entry.getSourceAttachmentPath());
				// XML example:
		      //  <classpathentry exported="true" kind="lib" path="bin/commerceservicesserver.jar" >
            //  <attributes>
            //          <attribute name="javadoc_location" value="https://download.hybris.com/api/5.5.1"/>
            //  </attributes>
				//  </classpathentry>
				
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
	private static boolean isJavaProject( IProject project ) 
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
}
