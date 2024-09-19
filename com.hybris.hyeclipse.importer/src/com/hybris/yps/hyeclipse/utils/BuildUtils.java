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
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;

import com.hybris.hyeclipse.commons.Constants;

public class BuildUtils {

	private static final String PLATFORM_BUILD_CONFIG = "yplatform_build";
	private static final String PLATFORM_CLEAN_BUILD_CONFIG = "yplatform_clean_build";
	
	private BuildUtils() {
		// inteded to be empty
	}

	/**
	 * Runs a ant build for project according to specified configuration
	 * 
	 * @param monitor
	 *            progress monitor
	 * @param cfg
	 *            build configuration name
	 * @param project
	 *            project to be build (for platform project will build whole
	 *            platform)
	 */
	public static void refreshAndBuild(IProgressMonitor monitor, String cfg, IProject project)
			throws InvocationTargetException {
		boolean isAutoBuildEnabled = isAutoBuildEnabled();
		enableAutoBuild(false);
		String projectLocation = project.getLocation().toString() + File.separator + "build.xml";
		String projectName = project.getName();
		if (!projectName.equals(Constants.PLATFROM)) {
			cfg = cfg + "_" + projectName;
		}
		try {

			ILaunchConfiguration launchCfg = getLaunchConfig(cfg);
			if (launchCfg == null) {
				if (cfg.contains(PLATFORM_BUILD_CONFIG)) {
					launchCfg = createAntBuildConfig(cfg, "all", projectLocation, projectName);
				} else if (cfg.contains(PLATFORM_CLEAN_BUILD_CONFIG)) {
					launchCfg = createAntBuildConfig(cfg, "clean,all", projectLocation, project.getName());
				}
			}
			
			if (launchCfg != null ) {
				launchCfg.launch(ILaunchManager.RUN_MODE, monitor);
			}

			if (cfg.equals(PLATFORM_CLEAN_BUILD_CONFIG)) {
				refreshWorkspaceAndBuild(monitor);
			} else if (!cfg.equals(PLATFORM_CLEAN_BUILD_CONFIG) && !cfg.equals(PLATFORM_BUILD_CONFIG)) { // NOSONAR
				refreshPlatformAndCurrentProject(project);
			}

		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}

		if (isAutoBuildEnabled) {
			enableAutoBuild(true);
		}
	}

	/**
	 * Creates an ant build configuration {@link ILaunchConfiguration}
	 * 
	 * @param configName
	 *            name of the configuration to be created
	 * @param targets
	 *            ant targets to be called
	 * @param buildPath
	 *            path to build.xml file
	 * @param projectName
	 *            name of the projects
	 * @return ant build configuration
	 */
	private static ILaunchConfiguration createAntBuildConfig(String configName, String targets, String buildPath,
			String projectName) throws CoreException {
		ILaunchConfiguration launchCfg;
		ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType("org.eclipse.ant.AntLaunchConfigurationType");
		ILaunchConfigurationWorkingCopy config = null;
		config = type.newInstance(null, configName);
		config.setAttribute("org.eclipse.ui.externaltools.ATTR_ANT_TARGETS", targets);
		config.setAttribute("org.eclipse.ui.externaltools.ATTR_CAPTURE_OUTPUT", true);
		config.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", buildPath);
		config.setAttribute("org.eclipse.ui.externaltools.ATTR_SHOW_CONSOLE", true);
		config.setAttribute("org.eclipse.ui.externaltools.ATTR_ANT_PROPERTIES", Collections.<String, String>emptyMap());
		config.setAttribute("org.eclipse.ant.ui.DEFAULT_VM_INSTALL", true);
		config.setAttribute("org.eclipse.jdt.launching.MAIN_TYPE",
				"org.eclipse.ant.internal.launching.remote.InternalAntRunner");
		config.setAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", projectName);
		config.setAttribute("org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER",
				"org.eclipse.ant.ui.AntClasspathProvider");
		config.setAttribute("process_factory_id", "org.eclipse.ant.ui.remoteAntProcessFactory");
		if (configName.equals(PLATFORM_BUILD_CONFIG) || configName.equals(PLATFORM_CLEAN_BUILD_CONFIG)) {
			config.setAttribute("org.eclipse.debug.core.ATTR_REFRESH_SCOPE", "${workspace}");
		}
		launchCfg = config.doSave();
		return launchCfg;
	}

	private static void refreshWorkspaceAndBuild(IProgressMonitor monitor) throws CoreException {
		// clean build might have created new directories (e.g. gensrc
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (FixProjectsUtils.isAHybrisExtension(project) && project.isOpen()) {
				IJavaProject javaProject = JavaCore.create(project);
				FixProjectsUtils.addSourceDirectoriesIfExisting(monitor, project, javaProject);
				FixProjectsUtils.removeSourceDirectoriesIfNotExisting(monitor, project, javaProject);
			}
		}

		// implement a build
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
	}

	/**
	 * Refreshes platform project and selected single extension project
	 * 
	 * @param project
	 *            single extension project to be refreshed
	 */
	private static void refreshPlatformAndCurrentProject(IProject project) throws CoreException {
		final IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM);
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		platformProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	protected static ILaunchConfiguration getLaunchConfig(String config) throws CoreException {
		for (ILaunchConfiguration launchCfg : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
			if (launchCfg.getName().equals(config)) {
				return launchCfg;
			}
		}
		return null;
	}

	protected static boolean isAutoBuildEnabled() {
		IPreferencesService service = Platform.getPreferencesService();
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		String key = "description.autobuilding";
		IScopeContext[] contexts = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE };
		return service.getBoolean(qualifier, key, false, contexts);
	}

	protected static void enableAutoBuild(boolean enable) {
		String qualifier = ResourcesPlugin.getPlugin().getBundle().getSymbolicName();
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(qualifier);
		node.putBoolean("description.autobuilding", enable);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			throw new IllegalStateException(e);
		}
	}
}
