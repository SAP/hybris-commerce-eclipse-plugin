package com.hybris.hyeclipse.script.executor.managers;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpResponseException;
import org.eclipse.core.resources.IFile;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

import com.hybris.hyeclipse.hac.manager.AbstractHACCommunicationManager;
import com.hybris.hyeclipse.hac.utils.ConsoleUtils;
import com.hybris.hyeclipse.hac.utils.PreferencesUtils;
import com.hybris.hyeclipse.script.executor.Activator;
import com.hybris.hyeclipse.script.executor.preferences.HACScriptExecutionPreferenceConstants;

/**
 * Manager for importing a script into the hAC
 */
public class ScriptExecutorManager extends AbstractHACCommunicationManager {

	/**
	 * HTPP request parameters for HAC
	 */
	private interface ScriptExecution {
		final String EXECUTE_URL = "/console/scripting/execute";
		interface Parameters {
			final String COMMIT_NAME = "commit";
			final String CONTENT_NAME = "script";
			final String TYPE_NAME = "scriptType";
		}
		interface Response {
			final String OUPUT_KEY = "outputText";
			final String RESULT_KEY = "executionResult";
			final String STACK_TRACE_KEY = "stacktraceText";
		}
	}
	
	/* Console strings */
	private final String RESULT_LABEL = "Result: " + ConsoleUtils.NEW_LINE;
	private final String OUTPUT_LABEL = "Output: " + ConsoleUtils.NEW_LINE;

	/**
	 * Imports script to the hAC with rollback mode.
	 * 
	 * @param scriptFile
	 *            script file to import
	 */
	public void importScript(final IFile scriptFile) {
		performScriptImport(scriptFile, false);
	}

	/**
	 * Imports and commit script to the hAC.
	 * 
	 * @param scriptFile
	 *            script file to import
	 */
	public void commitScript(final IFile scriptFile) {
		performScriptImport(scriptFile, true);
	}

	/**
	 * Performs script import.
	 * 
	 * @param scriptFile
	 *            script file to import
	 * @param commit
	 *            flat to determinate whether script will be committed or not.
	 */
	protected void performScriptImport(final IFile scriptFile, final Boolean commit) {
		updateLoginVariables();

		try {
			fetchCsrfTokenFromHac();
			loginToHac();
			try {
				fetchCsrfTokenFromHac();
				postScriptExecution(scriptFile, commit);
			} finally {
				logoutFromHac();
			}

		} catch (final IOException | AuthenticationException e) {
			ConsoleUtils.printError(e.getMessage());
		}
	}

	/**
	 * Send post request to hAC in order to execute script
	 * 
	 * @param scriptFile
	 *            File containing script to execute
	 * @param commit
	 *            indicate whether script will be committed.
	 * @return
	 * @throws HttpResponseException
	 * @throws IOException
	 */
	protected String postScriptExecution(final IFile scriptFile, final Boolean commit)
			throws HttpResponseException, IOException {
		final Map<String, String> parameters = new HashMap<>();
		final String scriptLanguage = getScriptByExtension(scriptFile.getFileExtension());

		parameters.put(ScriptExecution.Parameters.TYPE_NAME, scriptLanguage);
		parameters.put(ScriptExecution.Parameters.COMMIT_NAME, commit.toString());
		parameters.put(ScriptExecution.Parameters.CONTENT_NAME, getContentOfFile(scriptFile));

		final String response = sendPostRequest(ScriptExecution.EXECUTE_URL, parameters);

		displayScriptExecutionResult(response);
		return response;
	}

	/**
	 * Prints script execution result to the console
	 * 
	 * @param jsonResult
	 *            result of script import in JSON format.
	 */
	protected void displayScriptExecutionResult(final String jsonResult) {
		final JSONObject result = new JSONObject(jsonResult);
		final String output = result.getString(ScriptExecution.Response.OUPUT_KEY);
		final String stacktrace = result.getString(ScriptExecution.Response.STACK_TRACE_KEY);
		final String executionResult = result.getString(ScriptExecution.Response.RESULT_KEY);

		if (StringUtil.isBlank(stacktrace)) {
			ConsoleUtils.printMessage(RESULT_LABEL);
			ConsoleUtils.printMessage(executionResult);
			ConsoleUtils.printLine();
			ConsoleUtils.printMessage(OUTPUT_LABEL);
			ConsoleUtils.printMessage(output);
		} else {
			ConsoleUtils.printError(stacktrace);
			ConsoleUtils.printMessage(output);
		}
	}

	/**
	 * Returns script language name by it's file extension
	 * 
	 * @param fileExtension
	 *            script language file extension
	 * @return script language name by it's file extension
	 */
	@SuppressWarnings("unchecked")
	protected String getScriptByExtension(final String fileExtension) {
		final Optional<Serializable> preferenceObject = PreferencesUtils.readObjectFromStore(
				Activator.getDefault().getPreferenceStore(), HACScriptExecutionPreferenceConstants.P_SCRIPT_LANGUAGES);

		if (preferenceObject.isPresent()) {
			final Map<String, String> scriptLnaguages = (Map<String, String>) preferenceObject.get();

			final Optional<String> scriptLanguage = scriptLnaguages.entrySet().stream()
					.filter(entry -> Objects.equals(entry.getValue(), fileExtension)).map(Map.Entry::getKey)
					.findFirst();

			return scriptLanguage.orElse(null);
		}

		return null;
	}
}
