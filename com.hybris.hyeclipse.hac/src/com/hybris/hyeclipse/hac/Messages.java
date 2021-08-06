package com.hybris.hyeclipse.hac;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.hybris.hyeclipse.hac.messages"; //$NON-NLS-1$
	public static String HACPP_ALL_FIELDS;
	public static String HACPP_FIELD_EDITOR_VALUE;
	public static String HACPP_HAC_URL;
	public static String HACPP_HAC_VALID_URL_MSG;
	public static String HACPP_IMPORT_TIMEOUT;
	public static String HACPP_PASSWORD;
	public static String HACPP_SERVER_ENDPOINT;
	public static String HACPP_TIMEOUT_INTEGER_MSG;
	public static String HACPP_USERNAME;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
