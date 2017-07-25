package com.hybris.hyeclipse.junit;


import de.hybris.bootstrap.config.BootstrapConfigException;
import de.hybris.bootstrap.config.ExtensionInfo;
import de.hybris.bootstrap.config.PlatformConfig;
import de.hybris.bootstrap.config.SystemConfig;
import de.hybris.platform.testframework.HybrisJUnit4ClassRunner;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


public class DynamicClasspathHybrisJUnit4ClassRunner extends HybrisJUnit4ClassRunner
{
	private final File platformHome;
	private final SystemConfig systemConfig;
	private final PlatformConfig platformConfig;
	private Method addUrlMethod;



	public DynamicClasspathHybrisJUnit4ClassRunner(final Class<?> clazz) throws InitializationError
	{
		super(clazz);
		System.out.println("Initializing extensions");
		platformHome = getPlatformHome(this.getClass());
		systemConfig = SystemConfig.getInstanceByProps(loadProperties(platformHome));
		platformConfig = PlatformConfig.getInstance(systemConfig);
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see de.hybris.platform.testframework.HybrisJUnit4ClassRunner#run(org.junit. runner.notification.RunNotifier)
	 */
	@Override
	public void run(final RunNotifier notifier)
	{
		System.out.println("Start running...");
		if (isEclipseExecution())
		{
			final List<ExtensionInfo> extensions = platformConfig.getExtensionInfosInBuildOrder();
			for (final ExtensionInfo ext : extensions)
			{
				addClasspathToClassLoaderForExtension(ext);
			}
			// add db drivers
			addLibDirToClasspath(platformHome.getAbsolutePath() + "/lib/dbdriver");
		}
		super.run(notifier);
	}

	/**
	 * Add the /bin/xxxxserver.jar, classes and resources to the classpath for each extension (ExtensionInfo)
	 *
	 * @param ext
	 */
	private void addClasspathToClassLoaderForExtension(final ExtensionInfo ext)
	{
		final String extPath = ext.getExtensionDirectory().getAbsolutePath();
		// only add core extensions and the ones that are not already on
		// the class path
		if (!ext.isCoreExtension() && !StringUtils.contains(extPath, "/solrserver"))
		{
			// add the server jar e.g. catalog/bin/catalogserver.jar
			addURLtoClasspath(extPath + "/bin/" + ext.getName() + "server.jar");
			// add the classes directory (/classes or /eclipsebin/classes)
			addClassDirToClasspath(extPath);
			// add all jar files from the lib directory
			addLibDirToClasspath(extPath + "/lib");
			// add /resources directory
			final String resourcesDir = extPath + "/resources/";
			addURLtoClasspath(resourcesDir);
		}
	}

	/**
	 * If we have an eclipse class path (eclipsebin/classes) with files inside it then we use it rather than /classes.
	 * This allows the developer to see the changes in the extension without compiling in ant.
	 */
	protected void addClassDirToClasspath(final String extPath)
	{
		final File eclipseClasspath = new File(extPath + "/eclipsebin/classes");
		if (isEclipseExecution() && eclipseClasspath.exists() && eclipseClasspath.list().length > 0)
		{
			addURLtoClasspath(eclipseClasspath.getAbsolutePath());
		}
		addURLtoClasspath(extPath + "/classes/");
	}

	/**
	 * Add all jar files (*.jar) from the /lib directory of the extension to the classpath
	 */
	protected void addLibDirToClasspath(final String extPath)
	{
		final File libDirectory = new File(extPath);
		if (libDirectory.exists())
		{
			for (final File jar : libDirectory.listFiles())
			{
				if (jar.isFile() && FilenameUtils.getExtension(jar.getAbsolutePath()).equals("jar"))
				{
					addURLtoClasspath(jar.getAbsolutePath());
				}
			}
		}
	}

	protected void addURLtoClasspath(final String s)
	{
		final File f = new File(s);
		if (f.exists())
		{
			URL u = null;
			try
			{
				u = new URL("file:" + s);
			}
			catch (final MalformedURLException e)
			{
				throw new IllegalArgumentException(e);
			}
			try
			{
				getAddUrlMethodForClassLoader().invoke(ClassLoader.getSystemClassLoader(), u);
			}
			catch (final Exception e)
			{
				System.out.println("Could not add URL to classpath for url [" + u + "] " + e.getMessage());
			}
		}
	}

	protected File getPlatformHome(final Class clazz)
	{
		try
		{
			final URL e = clazz.getResource("/core-items.xml");
			final String rawBootstrapFile = URLDecoder.decode(e.getFile(), "UTF-8");
			final File bootstrapFile = new File(rawBootstrapFile);
			if (!bootstrapFile.exists())
			{
				throw new BootstrapConfigException("Can not find path to core-items.xml (" + bootstrapFile.getAbsolutePath() + ")");
			}
			else
			{
				return bootstrapFile.getParentFile().getParentFile().getParentFile().getParentFile();
			}
		}
		catch (final Exception e)
		{
			throw new BootstrapConfigException("Can not determine platformhome", e);
		}
	}

	protected static Hashtable<String, String> loadProperties(final File platformHome)
	{
		File file = new File(platformHome, "active-role-env.properties");
		if (!file.exists())
		{
			file = new File(platformHome, "env.properties");
			if (!file.exists())
			{
				throw new IllegalStateException("Could not find either " + platformHome + "/env.properties or " + platformHome
						+ "/active-role-env.properties, ensure you have built the platform before continuing");
			}
		}
		final Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("platformhome", platformHome.getAbsolutePath());
		final Properties properties = new Properties();
		InputStream in = null;
		try
		{
			in = new FileInputStream(file.getAbsolutePath());
			properties.load(in);
			in.close();
		}
		catch (final Exception e)
		{
			throw new IllegalArgumentException("Failed to load the properties for this platform", e);
		}
		finally
		{
			try
			{
				if (in != null)
				{
					in.close();
				}
			}
			catch (final IOException ie)
			{
				throw new IllegalArgumentException("Failed to close input stream after loading the properties for this platform", ie);
			}
		}
		for (final java.util.Map.Entry prop : properties.entrySet())
		{
			final String a = prop.getKey().toString();
			final String b = prop.getValue().toString();
			final String c = platformHome.getAbsolutePath();
			props.put(a, StringUtils.replace(b, "${platformhome}", c));
		}
		// hybris 5.7 additional properties
		props.put("HYBRIS_ROLES_DIR", platformHome.getAbsolutePath() + "/../../roles");
		props.put("HYBRIS_BOOTSTRAP_BIN_DIR", platformHome.getAbsolutePath() + "/bootstrap/bin");
		return props;
	}
	/**
	 * Finds the addURL method of the Class Loader and sets it to be acessible
	 */
	protected Method getAddUrlMethodForClassLoader()
	{
		if (addUrlMethod == null)
		{
			final URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			try
			{
				if (classLoader.getClass().equals(URLClassLoader.class))
				{
					addUrlMethod = classLoader.getClass().getDeclaredMethod("addURL", new Class[]
							{ URL.class });
				}
				else
				{
					addUrlMethod = classLoader.getClass().getSuperclass().getDeclaredMethod("addURL", new Class[]
							{ URL.class });
				}
				addUrlMethod.setAccessible(true);
			}
			catch (final Exception e)
			{
				throw new IllegalStateException("Could not get addURL method for class loader", e);
			}
		}
		return addUrlMethod;
	}
	protected boolean isEclipseExecution()
	{
		return System.getProperty("sun.java.command").contains("org.eclipse.jdt");
	}
}
