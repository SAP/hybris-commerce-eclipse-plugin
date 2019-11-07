package com.hybris.hyeclipse.script.executor.testers;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hybris.hyeclipse.commons.testers.AbstractFilePropertyTester;
import com.hybris.hyeclipse.commons.utils.PreferencesUtils;
import com.hybris.hyeclipse.script.executor.Activator;
import com.hybris.hyeclipse.script.executor.preferences.HACScriptExecutionPreferenceConstants;

/**
 * File property tester thats check whether the file extension is registered as
 * a script.
 */
public class ScriptPropertyTester extends AbstractFilePropertyTester {

	private final String PROPERTY_NAME = "isItImportableScript";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		return PROPERTY_NAME.equals(property) && testFileExtension(receiver);
	}

	/**
	 * Test if file extension matches either one of script languages extensions.
	 * 
	 * @param receiver
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	protected boolean testFileExtension(final Object receiver) {
		Object result;
		Map<String, String> scriptLanguages;
		result = PreferencesUtils
				.readObjectFromStore(Activator.getDefault().getPreferenceStore(), HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES)
				.orElse(null);
		if (result instanceof Map) {
			scriptLanguages = (Map<String, String>) result;
		} else {
			scriptLanguages = Collections.emptyMap();
		}

		Set<String> scriptsExtensions = scriptLanguages.entrySet().stream().map(Map.Entry::getValue)
				.collect(Collectors.toSet());

		if (!scriptsExtensions.isEmpty()) {
			return testSelectedFilesByExtensions(receiver, scriptsExtensions);
		}
		return false;
	}
}
