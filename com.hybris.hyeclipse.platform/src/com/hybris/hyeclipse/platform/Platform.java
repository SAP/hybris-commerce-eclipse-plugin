package com.hybris.hyeclipse.platform;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

import com.hybris.hyeclipse.platform.internal.Plugin;

public class Platform {
	
	public static final PlatformHolder holder = new PlatformHolder(Plugin.getDefault().getLog());
	
	private final IJavaProject platformJavaProject;
	private final String[] classPath;

	static boolean isPlatformProject(final IProject project) {
		try {
			return project.exists() && project.hasNature(JavaCore.NATURE_ID) && project.getName().equals("platform");
		} catch (CoreException e) {
			return false;
		}
	}

	Platform(IJavaProject platformJavaProject) throws CoreException {
		this.platformJavaProject = platformJavaProject;
		this.classPath = JavaRuntime.computeDefaultRuntimeClassPath(platformJavaProject);
	}
	
	IJavaProject getPlatformJavaProject() {
		return platformJavaProject;
	}
	
	public IContainer getPlatformHome() {
		return platformJavaProject.getProject();
	}
	
	public File getPlatformHomeFile() {
		return getPlatformHome().getLocation().toFile();
	}
	
	public List<String> getPlatformClassPath() {
		return Collections.unmodifiableList(Arrays.asList(classPath));
	}

}
