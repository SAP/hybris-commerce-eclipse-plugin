package com.hybris.yps.hyeclipse.wizards;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.hybris.yps.hyeclipse.wizards.messages"; //$NON-NLS-1$
	public static String error_on_import;
	public static String error_on_import_info;
	public static String ImportWizard_error_attaching_srcs;
	public static String ImportWizard_error_on_import;
	public static String ImportWizard_invalid_platform_dir;
	public static String ImportWizard_invalid_platform_dir_info;
	public static String ImportWizard_removing_extension;
	public static String ImportWizard_wrong_src_zip;
	public static String ImportWizard_wrong_src_zip_info;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
