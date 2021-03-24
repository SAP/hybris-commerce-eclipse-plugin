package com.hybris.yps.hyeclipse;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import com.hybris.yps.hyeclipse.utils.Importer;

public class SAPCommerceProjectConfigurator implements ProjectConfigurator {

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		PathMatcher extensionMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/" + Importer.HYBRIS_EXTENSION_FILE);
		PathMatcher configMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/" + Importer.LOCAL_EXTENSION_FILE);
		final Set<java.nio.file.Path> projectFiles = new HashSet<>();
		try (Stream<java.nio.file.Path> stream = Files.walk(root.getParentFile().toPath(), 4, FileVisitOption.FOLLOW_LINKS)) {
			projectFiles.addAll(stream.filter(extensionMatcher::matches).collect(Collectors.toSet()));
		} catch (IOException e) {
			e.printStackTrace();
			// swallow exception
		}
		try (Stream<java.nio.file.Path> stream = Files.walk(root.getParentFile().getParentFile().toPath(), 3, FileVisitOption.FOLLOW_LINKS)) {
			Optional<java.nio.file.Path> configFolder = stream.filter(configMatcher::matches).findFirst();
			configFolder.ifPresent(a -> projectFiles.add(a));
		} catch (IOException e) {
			e.printStackTrace();
			// swallow exception
		}
		Set<File> res = new HashSet<>();
		for (java.nio.file.Path projectFile : projectFiles) {
			res.add(projectFile.toFile().getParentFile());
		}
		return res;
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(new Path(Importer.HYBRIS_EXTENSION_FILE)).exists() || container.getFile(new Path(Importer.LOCAL_EXTENSION_FILE)).exists();
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> folders = new HashSet<>();
		folders.add(project.getFolder("classes"));
		return folders;
	}
	
	@Override
	public void removeDirtyDirectories(Map<File, List<ProjectConfigurator>> proposals) {
		List<File> removal = proposals.keySet().stream().filter(f -> f.getAbsolutePath().contains("configtemplate")).collect(Collectors.toList());
		for (File file : removal) {
			proposals.remove(file);
		}
	}

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
		return true;
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredPaths, IProgressMonitor monitor) {
//		try {
//			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
//		} catch (CoreException ex) {
//			Activator.getDefault().log(ex.getMessage(), ex);
//		}

	}

}
