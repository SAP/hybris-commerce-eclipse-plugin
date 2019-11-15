package com.hybris.hyeclipse.ytypesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.prefs.Preferences;

import com.hybris.yps.hyeclipse.ExtensionHolder;

import de.hybris.bootstrap.config.ExtensionInfo;
import de.hybris.bootstrap.config.PlatformConfig;
import de.hybris.bootstrap.config.SystemConfig;
import de.hybris.bootstrap.typesystem.YAttributeDescriptor;
import de.hybris.bootstrap.typesystem.YType;
import de.hybris.bootstrap.typesystem.YTypeSystem;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.hybris.hyeclipse.ytypesystem"; //$NON-NLS-1$

	public static final String YBOOTSTRAP_PLUGIN_ID = "com.hybris.hyeclipse.ybootstrap";

	// The shared instance
	private static Activator plugin;

	private Bundle ybootstrapBundle;
	private File platformHome;
	private SystemConfig systemConfig;
	private PlatformConfig platformConfig;
	private YTypeSystem typeSystem;
	private Set<? extends YType> allTypes;
	private List<String> allTypeNames;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;

		if (getPlatformHome() != null) {
			loadBootstrapBundle(bundleContext);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		super.stop(bundleContext);
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

	public Bundle getYbootstrapBundle() {
		return ybootstrapBundle;
	}

	public void setYbootstrapBundle(Bundle ybootstrapBundle) {
		this.ybootstrapBundle = ybootstrapBundle;
	}

	public File getPlatformHome() {
		if (platformHome == null) {

			// Get platform home from workspace preferences
			Preferences preferences = InstanceScope.INSTANCE.getNode("com.hybris.hyeclipse.preferences");
			String platformHomeStr = preferences.get("platform_home", null);
			if (platformHomeStr == null) {
				IProject platformProject = ResourcesPlugin.getWorkspace().getRoot().getProject("platform");
				IPath platformProjectPath = platformProject.getLocation();
				if (platformProjectPath != null) {
					setPlatformHome(platformProjectPath.toFile());
				}
			} else {
				setPlatformHome(new File(platformHomeStr));
			}
		}
		return platformHome;
	}

	public void setPlatformHome(File platformHome) {
		this.platformHome = platformHome;
	}

	public void unloadBootstrapBundle() {
		if (ybootstrapBundle != null) {
			try {
				ybootstrapBundle.uninstall();
				ybootstrapBundle = null;
			} catch (BundleException e) {
				logError("couldn't unload bootstrap", e);
			}
		}
	}

	public void loadBootstrapBundle(BundleContext context) {
		
		List<String> items = Arrays.asList("ybootstrap/log4j-1.2.17.jar", "ybootstrap/commons-collections-3.2.2.jar", "ybootstrap/META-INF");
		
		Bundle bundle = getDefault().getBundle();
		
		//Build zip containing ybootstrap, log4j and MANIFEST.MF
		List<File> sources = new ArrayList<>();
		Path p = Paths.get(getPlatformHome().getAbsolutePath(), "bootstrap", "bin", "ybootstrap.jar");
		if (!p.toFile().exists()) {
			logError(
					MessageFormat.format("could not find ybootstrap in platform folder {0}",
							getPlatformHome().getAbsolutePath()),
					new URISyntaxException(p.toString(), "could not resolve path"));
			sources.add(p.toFile());
		}
		
		try {
			for (String item : items) {
					sources.add(entryToFile(item));
			}
			ByteArrayOutputStream baos = BundlePackager.buildZip(sources);
			byte[] bytes = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			
			setYbootstrapBundle(bundle.getBundleContext().installBundle(YBOOTSTRAP_PLUGIN_ID, bais));

		} catch (URISyntaxException | IOException e) {
			logError("URISyntaxException | IOException", e);
		}
		catch (BundleException e) {
			logError("BundleException", e);
		}		
	}

	private File entryToFile(String url) throws IOException, URISyntaxException {
		Bundle bundle = getDefault().getBundle();
		return Paths.get(FileLocator.toFileURL(bundle.getEntry(url)).toURI()).toFile();
	}

	public SystemConfig getSystemConfig() {
		if (systemConfig == null) {

			Hashtable<String, String> props = null;
			try {
				props = loadProperties(getPlatformHome());
				Field singletonField = SystemConfig.class.getDeclaredField("singleton");
				singletonField.setAccessible(true);
				singletonField.set(this, null);
				Field instanceField = PlatformConfig.class.getDeclaredField("instance");
				instanceField.setAccessible(true);
				instanceField.set(this, null);
			} catch (NoSuchFieldException | SecurityException | IOException | IllegalArgumentException
					| IllegalAccessException e) {
				logError("(NoSuchField|IO|Security|IllegalArgument|IllegalAccess)Exception", e);
			} catch (java.lang.UnsupportedClassVersionError err) {
				final Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"Incompatible JVM for IDE and SAP Commerce version", err);
				StatusManager.getManager().handle(status);

				printError("Incompatible JVM for IDE and SAP Commerce version",
						"eclipse IDE has to run on java version equal or greater than supported by SAP Commerce. More information how to change VM for IDE is available here: https://wiki.eclipse.org/Eclipse.ini",
						status);
			}

			systemConfig = SystemConfig.getInstanceByProps(props);
		}
		return systemConfig;
	}

	private void printError(String title, String description, Status status) {
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {

			Shell parent = PlatformUI.getWorkbench().getDisplay().getActiveShell(); // get shell.
			ErrorDialog.openError(parent, title, description, status);
		});
	}

	public void nullifySystemConfig() {
		systemConfig = null;
	}

	public PlatformConfig getPlatformConfig() {
		if (platformConfig == null) {
			platformConfig = PlatformConfig.getInstance(getSystemConfig());
		}
		return platformConfig;
	}

	public void nullifyPlatformConfig() {
		platformConfig = null;
	}

	public YTypeSystem getTypeSystem() {
		if (typeSystem == null) {
			typeSystem = YTypeSystemBuilder.buildTypeSystem();
		}
		return typeSystem;
	}

	public void nullifyTypeSystem() {
		typeSystem = null;
	}

	public String getConfigDirectory() {
		SystemConfig systemConfig = getPlatformConfig().getSystemConfig();
		if (systemConfig != null) {
			return systemConfig.getConfigDir().getAbsolutePath();
		}
		return null;
	}

	public Set<? extends YType> getAllTypes() {
		if (allTypes == null) {
			allTypes = getTypeSystem().getTypes();
		}
		return allTypes;
	}

	public void nullifyAllTypes() {
		allTypes = null;
	}

	public List<String> getAllTypeNames() {
		if (allTypeNames == null) {
			Set<? extends YType> allTypes = getAllTypes();
			allTypeNames = new ArrayList<String>(allTypes.size());
			for (YType type : allTypes) {
				allTypeNames.add(type.getCode());
			}
		}
		return allTypeNames;
	}

	public void nullifyAllTypeNames() {
		allTypeNames = null;
	}

	public List<String> getAllAttributeNames(String typeName) {
		Set<YAttributeDescriptor> typeAttributes = getTypeSystem().getAttributes(typeName);
		List<String> allAttributeNames = new ArrayList<String>(typeAttributes.size());
		for (YAttributeDescriptor attribute : typeAttributes) {
			allAttributeNames.add(attribute.getQualifier());
		}
		return allAttributeNames;
	}

	public String getAttributeName(String typeName, String potentialAttributeName) {
		YType type = getTypeSystem().getType(typeName);
		YAttributeDescriptor attribute = getTypeSystem().getAttribute(type.getCode(), potentialAttributeName);
		if (attribute != null) {
			return attribute.getQualifier();
		}
		return null;
	}

	public String getTypeLoaderInfo(String typeName) {
		YType type = getTypeSystem().getType(typeName);
		if (type != null) {
			return type.getLoaderInfo();// core-items.xml:2793(ItemTypeTagListener)
		}
		return "";
	}

	public Set<ExtensionHolder> getAllExtensionsForPlatform() {
		Set<ExtensionHolder> allExtensions = new HashSet<ExtensionHolder>();
		List<ExtensionInfo> allExtensionInfos = getPlatformConfig().getExtensionInfosInBuildOrder();
		for (ExtensionInfo extension : allExtensionInfos) {
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

	private ExtensionHolder createExtensionHolderFromExtensionInfo(ExtensionInfo extension) {

		ExtensionHolder extHolder = null;
		if (!extension.isCoreExtension()) {

			// some extensions appear to not have a directory so we skip them
			if (extension.getExtensionDirectory() == null) {
				log("extension [" + extension.getName() + "] doesn't have an extension directory, skipping");
				return null;
			}
			String path = extension.getExtensionDirectory().getAbsolutePath();
			extHolder = new ExtensionHolder(path, extension.getName());
			if (extension.getCoreModule() != null) {
				extHolder.setCoreModule(true);
			}
			if (extension.getWebModule() != null) {
				extHolder.setWebModule(true);
			}
			if (extension.getHMCModule() != null && getPlatformConfig().getExtensionInfo("hmc") != null) {
				extHolder.setHmcModule(true);
			}

			extHolder.setBackofficeModule(false);
			String backOfficeMeta = extension.getMeta("backoffice-module");
			if (backOfficeMeta != null && backOfficeMeta.equalsIgnoreCase("true")) {
				extHolder.setBackofficeModule(true);
			}

			extHolder.setAddOnModule(false);
			File addonDir = new File(path, "acceleratoraddon");
			if (addonDir.exists() && addonDir.isDirectory()) {
				extHolder.setAddOnModule(true);
			}

			File libDir = new File(path, "lib");
			if (libDir.exists() && libDir.isDirectory()) {
				File[] files = libDir.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase(Locale.ENGLISH).endsWith(".jar");
					}
				});
				for (File file : files) {
					extHolder.getJarFiles().add(file.getName());
				}
			}

			if (!extension.getAllRequiredExtensionNames().isEmpty()) {
				List<String> extensions = new LinkedList(extension.getAllRequiredExtensionNames());
				if (!extensions.contains("platform")) {
					extensions.add("platform");
				}
				extHolder.setDependentExtensions(extensions);
			}

		}

		return extHolder;
	}

	private static Hashtable<String, String> loadProperties(File platformHome)
			throws FileNotFoundException, IOException {

		File file = new File(platformHome, "active-role-env.properties");
		if (!file.exists()) {
			file = new File(platformHome, "env.properties");
			if (!file.exists()) {
				throw new IllegalStateException(
						"Could not find either " + platformHome + "/env.properties or " + platformHome
								+ "/active-role-env.properties, ensure you have built the platform before continuing");
			}
		}

		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("platformhome", platformHome.getAbsolutePath());
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(file.getAbsolutePath())) {
			properties.load(in);
		}

		for (Entry<?, ?> prop : properties.entrySet()) {

			String a = prop.getKey().toString();
			String b = prop.getValue().toString();
			String c = platformHome.getAbsolutePath();

			props.put(a, StringUtils.replace(b, "${platformhome}", c));
		}

		// hybris 5.7 additional properties
		props.put("HYBRIS_ROLES_DIR", platformHome.getAbsolutePath() + "/../../roles");
		props.put("HYBRIS_BOOTSTRAP_BIN_DIR", platformHome.getAbsolutePath() + "/bootstrap/bin");

		return props;
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
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Status.ERROR, msg, e);
		} else {
			status = new Status(Status.INFO, Activator.PLUGIN_ID, Status.OK, msg, e);
		}
		getLog().log(status);
	}

}
