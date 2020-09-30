package com.hybris.hyeclipse.tsv;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.hybris.hyeclipse.tsv.messages"; //$NON-NLS-1$
	public static String TSVPracticesError;
	public static String TSVPracticesLink;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
