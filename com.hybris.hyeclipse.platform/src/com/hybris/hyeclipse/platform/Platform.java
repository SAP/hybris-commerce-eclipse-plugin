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

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.platform.internal.Plugin;

public class Platform {
	
	public static final PlatformHolder holder = new PlatformHolder(Plugin.getDefault().getLog());
	
	private final IJavaProject platformJavaProject;
	private final String[] classPath;

	static boolean isPlatformProject(final IProject project) {
		try {
			return project.exists() && project.hasNature(JavaCore.NATURE_ID) && project.getName().equals(Constants.PLATFROM);
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
