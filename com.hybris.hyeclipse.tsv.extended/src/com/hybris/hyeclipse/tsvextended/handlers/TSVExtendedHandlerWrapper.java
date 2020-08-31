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
