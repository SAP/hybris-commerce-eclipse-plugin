package com.hybris.hyeclipse.tsvextended.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.hybris.hyeclipse.platform.Platform;
import com.hybris.hyeclipse.tsv.Activator;

public class TSVExtendedHandlerWrapper extends AbstractHandler {
	
	private Platform currentPlatform;
	private URLClassLoader currentPlatformDependentLoader;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			final URLClassLoader classLoader = getCurrentPlatformDependentLoader();
			final ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(classLoader);
				((Runnable) classLoader
						.loadClass("com.hybris.hyeclipse.tsvextended.platform.handlers.TSVExtendedHandler")
						.getConstructor(new Class[0]).newInstance()).run();
				return null;
			} finally {
				Thread.currentThread().setContextClassLoader(oldTccl);
			}
		} catch (IOException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| ClassNotFoundException e) {
			throw new ExecutionException("Failed to run extended TSV analysis", e);
		} catch (InvocationTargetException e) {
			throw new ExecutionException("Failed to run extended TSV analysis", e.getTargetException());
		}
	}
	
	private URLClassLoader getCurrentPlatformDependentLoader() throws IOException {
		while (currentPlatform != Platform.holder.getCurrent()) {
			if (currentPlatformDependentLoader != null) {
				currentPlatformDependentLoader.close();
				currentPlatformDependentLoader = null;
			}
			
			currentPlatform = Platform.holder.getCurrent();
			if (currentPlatform != null) {
				currentPlatformDependentLoader = new URLClassLoader(getClassPath(), getClass().getClassLoader());
			}
		}
		return currentPlatformDependentLoader;
	}

	private URL[] getClassPath() throws MalformedURLException {
		assert currentPlatform != null;
		
		final List<URL> classPathUrls = new LinkedList<>();

		classPathUrls.add(Activator.getDefault().getBundle().getEntry("/platform-dependent.jar"));
		classPathUrls.add(Activator.getDefault().getBundle().getEntry("/lib/tsv-extended-0.0.1-SNAPSHOT.jar"));

		for (final String cpEntry : currentPlatform.getPlatformClassPath()) {
			//do not include conflicting libraries
			if (!cpEntry.contains("groovy")) {
				classPathUrls.add(new File(cpEntry).toURI().toURL());
			}
		}
		
		return classPathUrls.toArray(new URL[classPathUrls.size()]);
	}

}
