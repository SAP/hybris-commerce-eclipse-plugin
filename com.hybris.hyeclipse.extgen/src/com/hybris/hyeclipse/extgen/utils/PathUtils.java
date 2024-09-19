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
package com.hybris.hyeclipse.extgen.utils;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.hybris.hyeclipse.commons.Constants;

/**
 * Utility class around platform path handling.
 */
public class PathUtils {
	private static final String DEFAULT_EXTENSION_DIR = "custom";
	private static final String LOCAL_EXTENSIONS_PATH = "config/localextensions.xml";
	private static final String TEMPLATE_DIR = "ext-template";

	/**
	 * Returns path to default extension directory
	 * 
	 * @param saveLocation
	 *            custom save location of extension
	 * @param extName
	 *            extension name
	 * @return extension path
	 */
	public static String getCustomExtensionPath(File saveLocation, String extName) {
		return saveLocation.getAbsolutePath() + File.separator + extName;
	}

	/**
	 * Returns path to default extension directory
	 * 
	 * @param extName
	 *            extension name
	 * @return extension path
	 */
	public static String getDefaultExtensionPath(String extName) {
		return getDefaultExtensionDirPath() + File.separator + extName;
	}

	/**
	 * Returns path to default extensions directory
	 * 
	 * @return extensions directory path
	 */
	public static String getDefaultExtensionDirPath() {
		String binPath = new File(getPlatformPath()).getParent() + File.separator;
		return binPath + DEFAULT_EXTENSION_DIR;
	}

	/**
	 * Returns path to localextensions.xml
	 * 
	 * @return localextensions.xml path
	 */
	public static String getLocalExtensionsPath() {
		String hybrisPath = new File(getPlatformPath()).getParentFile().getParent() + File.separator;
		return hybrisPath + LOCAL_EXTENSIONS_PATH;
	}

	/**
	 * Returns path to ext-template directory
	 * 
	 * @return ext-template path
	 */
	public static String getExtensionsTemplatePath() {
		String binPath = new File(getPlatformPath()).getParent() + File.separator;
		return binPath + TEMPLATE_DIR + File.separator;
	}

	/**
	 * Returns path to platform project directory
	 * 
	 * @return path to platform project directory
	 */
	public static String getPlatformPath() {
		IPath platformProjectPath = ResourcesPlugin.getWorkspace().getRoot().getProject(Constants.PLATFROM)
				.getLocation();
		return platformProjectPath.toFile().getAbsolutePath();
	}
}
