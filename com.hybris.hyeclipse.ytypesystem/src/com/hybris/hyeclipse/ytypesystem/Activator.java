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
package com.hybris.hyeclipse.ytypesystem;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.config.ExtensionConfig;
import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;
import com.hybris.hyeclipse.commons.typesystem.YType;
import com.hybris.yps.hyeclipse.ExtensionHolder;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.ytypesystem"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private File platformHome;

	public Activator() {
		super();
		if (plugin == null) {
			plugin = this; //NOSONAR			
		}
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative
	 * path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public File getPlatformHome() {
		if (platformHome == null) {
			// Get platform home from workspace preferences
			File platformFile = com.hybris.hyeclipse.commons.Activator.resetPlatformBootstrapBundle();
			setPlatformHome(platformFile);
		}
		return platformHome;
	}

	public void setPlatformHome(File platformHome) {
		this.platformHome = platformHome;
	}

	public SystemConfig getSystemConfig() {
		final File home = getPlatformHome();
		if (home == null) {
			throw new IllegalStateException("No Commerce platform selected; import a platform first");
		}
		try {
			return com.hybris.hyeclipse.commons.Activator.getDefault().getSystemConfigRepository().find(home)
					.orElseThrow(() -> new IllegalStateException(
							"System configuration is unavailable; rerun the platform import"));
		} catch (CoreException e) {
			throw new IllegalStateException(e.getStatus().getMessage(), e);
		}
	}

	/** @return the shared generated-model snapshot */
	public TypeSystemSnapshot getTypeSystemSnapshot() {
		return com.hybris.hyeclipse.commons.Activator.getDefault().getTypeSystemIndex().getSnapshot();
	}

	public String getConfigDirectory() {
		SystemConfig sysConfig = getSystemConfig();
		if (sysConfig != null) {
			return sysConfig.getConfigDir().getAbsolutePath();
		}
		return null;
	}

	public Set<YType> getAllTypes() {
		return new HashSet<>(getTypeSystemSnapshot().getTypes());
	}

	public List<String> getAllTypeNames() {
		return getTypeSystemSnapshot().getAllTypeNames();
	}

	public List<String> getAllAttributeNames(String typeName) {
		return getTypeSystemSnapshot().getAllAttributeNames(typeName);
	}

	public String getAttributeName(String typeName, String potentialAttributeName) {
		YAttributeDescriptor attribute = getTypeSystemSnapshot().getAttribute(typeName, potentialAttributeName);
		if (attribute != null) {
			return attribute.getQualifier();
		}
		return null;
	}

	public Set<ExtensionHolder> getAllExtensionsForPlatform() {
		Set<ExtensionHolder> allExtensions = new HashSet<>();
		List<ExtensionConfig> allExtensionInfos = getSystemConfig().getExtensionsInOrder();
		for (ExtensionConfig extension : allExtensionInfos) {
			// sanity check, should never be null
			if (extension != null) {
				ExtensionHolder extHolder = createExtensionHolderFromExtensionInfo(extension);
				if (extHolder != null) {
					allExtensions.add(extHolder);
				}
			}
		}
		return allExtensions;
	}

	private ExtensionHolder createExtensionHolderFromExtensionInfo(ExtensionConfig extension) {

		ExtensionHolder extHolder = null;
		
		if (extension.isPlatformExtension()) {
			return extHolder;
		}

		// some extensions appear to not have a directory so we skip them
		if (extension.getExtensionDirectory() == null) {
			log("extension [" + extension.getName() + "] doesn't have an extension directory, skipping");
			return null;
		}
		String path = extension.getExtensionDirectory().getAbsolutePath();
		extHolder = new ExtensionHolder(path, extension.getName());
		if (extension.isCoreModule()) {
			extHolder.setCoreModule(true);
		}
		if (extension.isWebModule()) {
			extHolder.setWebModule(true);
		}
		if (extension.isHmcModule() && getSystemConfig().getExtension("hmc") != null) {
			extHolder.setHmcModule(true);
		}

		extHolder.setBackofficeModule(extension.isBackofficeModule());

		extHolder.setAddOnModule(false);
		File addonDir = new File(path, "acceleratoraddon");
		if (addonDir.exists() && addonDir.isDirectory()) {
			extHolder.setAddOnModule(true);
		}

		File libDir = new File(path, "lib");
		if (libDir.exists() && libDir.isDirectory()) {
			File[] files = libDir.listFiles((File dir, String name) -> 
				name.toLowerCase(Locale.ENGLISH).endsWith(".jar")
			);
			if (files != null) {
				for (File file : files) {
					extHolder.getJarFiles().add(file.getName());
				}
			}
		}

		if (!extension.getDependencies().isEmpty()) {
			List<String> extensions = new LinkedList<>(extension.getDependencies());
			if (!extensions.contains(Constants.PLATFROM)) {
				extensions.add(Constants.PLATFROM);
			}
			extHolder.setDependentExtensions(extensions);
		}

		return extHolder;
	}

	public static void log(String msg) {
		getDefault().log(msg, null);
	}

	public static void logError(String msg, Exception e) {
		getDefault().log(msg, e);
	}

	public void log(String msg, Exception e) {
		Status status = null;
		if (e != null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, msg, e);
		} else {
			status = new Status(IStatus.INFO, Activator.PLUGIN_ID, IStatus.OK, msg, e);
		}
		getLog().log(status);
	}

}
