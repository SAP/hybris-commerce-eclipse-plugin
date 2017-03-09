package com.hybris.hyeclipse.emf.beans.presentation.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class BeansEditorPlugin extends AbstractUIPlugin {
	
	private static String PLUGIN_ID = "com.hybris.hyeclipse.emf.beans.editor";
	
	private static BeansEditorPlugin INSTANCE;
	
	public static BeansEditorPlugin getDefault() {
		return INSTANCE;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		INSTANCE = this;
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		INSTANCE = null;
		super.stop(context);
	}

	public static void logError(String message, Throwable ex) {
		BeansEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, ex));
	}

}
