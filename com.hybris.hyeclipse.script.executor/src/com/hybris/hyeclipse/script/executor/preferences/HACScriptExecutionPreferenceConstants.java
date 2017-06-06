package com.hybris.hyeclipse.script.executor.preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Class contains constants for hAC script execution preference page. 
 */
public abstract class HACScriptExecutionPreferenceConstants {

	/**
	 * Default values of script languages
	 */
	private interface ScriptLanguages {
		interface Groovy {
			final static String NAME = "groovy";
			final static String EXTENSION = "groovy";
		}
		interface BeanShell {
			final static String EXTENSION = "bsh";
			final static String NAME = "beanshell";
		}
		interface JavaScript {
			final static String EXTENSION = "js";
			final static String NAME = "javascript";
		}	
	}
		
	/** Preferences */
	public static final String P_SCRIPT_LANGUAGES = "scriptLanguages";
	
	/** DEFAULT VALUES */
	@SuppressWarnings("serial")
	public final static Map<String, String> DEFAULT_SCRIPT_LANGUAGES = new HashMap<String, String>() {
		{
			put(ScriptLanguages.Groovy.NAME, ScriptLanguages.Groovy.EXTENSION);
			put(ScriptLanguages.BeanShell.NAME, ScriptLanguages.BeanShell.EXTENSION);
			put(ScriptLanguages.JavaScript.NAME, ScriptLanguages.JavaScript.EXTENSION);
		}
	};
}
