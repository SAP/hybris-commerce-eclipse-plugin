package com.hybris.hyeclipse.script.executor.testers;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hybris.hyeclipse.utils.PreferencesUtils;
import com.hybris.hyeclipse.script.executor.Activator;
import com.hybris.hyeclipse.script.executor.preferences.HACScriptExecutionPreferenceConstants;
import com.hybris.hyeclipse.testers.AbstractFilePropertyTester;

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
	 * @param receiver
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected boolean testFileExtension(final Object receiver) {
		Optional<Serializable> scriptPreferenceValue = PreferencesUtils.readObjectFromStore(
				Activator.getDefault().getPreferenceStore(), HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES);
	
		Map<String, String> scriptLanguages = (Map<String, String>) scriptPreferenceValue.get();
		Set<String> scriptsExtensions = scriptLanguages.entrySet().stream().map(Map.Entry::getValue)
				.collect(Collectors.toSet());

		
		if( scriptPreferenceValue.isPresent() ) {
			return testSelectedFilesByExtensions(receiver, scriptsExtensions);
		}
		
		return false;
	}
}
