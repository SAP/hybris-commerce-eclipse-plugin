package com.hybris.hyeclipse.script.executor.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.hybris.hyeclipse.script.executor.preferences.messages"; //$NON-NLS-1$
	public static String HACScriptExecutionPreferencePage_Buttons_Add;
	public static String HACScriptExecutionPreferencePage_Buttons_Modify;
	public static String HACScriptExecutionPreferencePage_Buttons_Remove;
	public static String ScriptLanguageDialog_Add_Lang;
	public static String ScriptLanguageDialog_Cannot_Blank;
	public static String ScriptLanguageDialog_Duplicated_File_Ext;
	public static String ScriptLanguageDialog_Duplicated_Lang;
	public static String ScriptLanguageDialog_File_Extension;
	public static String ScriptLanguageDialog_Lang_Cannot_Blank;
	public static String ScriptLanguageDialog_Modify_Script_Lang;
	public static String ScriptLanguageDialog_Script_Lang;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
