package com.hybris.yps.hyeclipse;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.xml.sax.SAXException;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.utils.XmlScannerUtils;

public class SAPCommerceProjectConfigurator implements ProjectConfigurator {
	
	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		PathMatcher extensionMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/" + Constants.EXTENSION_INFO_XML);
		PathMatcher configMatcher = FileSystems.getDefault().getPathMatcher("glob:/**/" + Constants.LOCAL_EXTENSIONS_XML);
		Set<String> configuredExtensions = new HashSet<>();
		final Set<java.nio.file.Path> projectFiles = new HashSet<>();
		try (Stream<java.nio.file.Path> stream = Files.walk(root.getParentFile().toPath(), 6, FileVisitOption.FOLLOW_LINKS)) {
			projectFiles.addAll(stream.filter(extensionMatcher::matches).collect(Collectors.toSet()));
		} catch (IOException e) {
			Activator.logError("could not access file", e);
			// swallow exception
		}
		try (Stream<java.nio.file.Path> stream = Files.walk(root.getParentFile().getParentFile().toPath(), 3, FileVisitOption.FOLLOW_LINKS)) {
			Optional<java.nio.file.Path> configFolder = stream.filter(configMatcher::matches).findFirst();
			configFolder.ifPresent(a -> {
				try {
					projectFiles.add(a);
					configuredExtensions.addAll(XmlScannerUtils.getLocalExtensions(a));
				} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
					Activator.logError(MessageFormat.format("could not parse XML file {0}", a), e);
				}
			});
			
		} catch (IOException e) {
			Activator.logError("could not access file", e);
		}
		Set<File> res = new HashSet<>();
		for (java.nio.file.Path projectFile : projectFiles) {
			File pf = projectFile.toFile().getParentFile();
			if (configuredExtensions.contains(pf.getName().toLowerCase())) {
				res.add(pf);
			}
		}
		return res;
	}

	/**
	 * Method checks if given folder contains <pre>extensioninfo.xml</pre> for extensions,
	 * <pre>localextensions.xml</pre> for config folder,
	 * <pre>extensions.xml</pre> for <pre>platform</pre> extension.
	 */
	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(new Path(Constants.EXTENSION_INFO_XML)).exists() || container.getFile(new Path(Constants.LOCAL_EXTENSIONS_XML)).exists() || container.getFile(new Path(Constants.EXTENSIONS_XML)).exists();
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
		IProjectDescription description;
		try {
			description = project.getDescription();
			final Set<String> natSet = new HashSet<>(Arrays.asList(description.getNatureIds()));
			natSet.add(JavaCore.NATURE_ID);
			final String[] newNatures = natSet.toArray(new String[natSet.size()]);
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
			project.refreshLocal(2, monitor);
		} catch (CoreException e) {
			Activator.logError(String.format("could not access project description for %s. Skipping", project.getName()), e);
		}

	}

}
