package com.hybris.hyeclipse.tsv;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.hybris.hyeclipse.tsv.messages"; //$NON-NLS-1$
	public static String RunTSVWizard_error;
	public static String RunTSVWizard_taskCreating;
	public static String RunTSVWizard_taskOpening;
	public static String RunTSVWizard_TSVTitle;
	public static String TSVExtendedHandlerWrapper_classLoaderNullError;
	public static String TSVExtendedHandlerWrapper_runError;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
