package com.hybris.hyeclipse.impex.importer.managers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eclipse.core.resources.IFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.hybris.hyeclipse.hac.manager.AbstractHACCommunicationManager;
import com.hybris.hyeclipse.hac.utils.ConsoleUtils;

/**
 * Class responsible for handling the connections to the {@link #getEndpointUrl()} and importing impex file. 
 */
public class ImportManager extends AbstractHACCommunicationManager {

	/**
	 * Impex import HAC parameters
	 */
	private interface ImpexImport {
		final String IMPEX_IMPORT_PATH = "/console/impex/import";
		
		interface Parameters {
			final String ENCODING = "encoding";
			final String MAX_THREADS = "maxThreads";
			final String SCRIPT_CONTENT = "scriptContent";
			final String VALIDATION_ENUM = "validationEnum"; 
			final String MAX_THREADS_VALUE = "4";
			final String VALIDATION_ENUM_VALUE = "IMPORT_STRICT";
		}
	}
	
	/**
	 * Meta data for impex import
	 */
	private interface Meta {
		final String IMPEX_IMPORT_ERROR_RESULT_ELEMENT = "pre";
		final String IMPEX_IMPORT_RESULT_SPAN_ATTR = "data-result";
		final String IMPEX_IMPORT_RESULT_SPAN = "span[id='impexResult']";
	}
	
	/**
	 * Messages for impex import
	 */
	private interface Messages {
		static final String EMPTY_FILE_ERROR = "File cannot be empty.";
		static final String CHECK_CONSOLE = "\nFor details check the console.";
		static final String IMPORT_ERROR = "Import has encountered problems.";	
	}
	
	public ImportManager() {
		super();
	}

	/**
	 * Performs an import of the impex file
	 *
	 * @param file
	 *            file to be imported
	 * @return message with import result
	 */
	public String performImport(final IFile impexFile) {
		updateLoginVariables();
		String resultMessage;
		try {
			fetchCsrfTokenFromHac();
			loginToHac();
			// continues only if logged in successfully
			try {
				fetchCsrfTokenFromHac();
				resultMessage = postImpex(impexFile);
			} finally {
				logoutFromHac();
			}
		} catch (final IOException | AuthenticationException e) {
			resultMessage = e.getMessage();
		}
		return resultMessage;
	}

	/**
	 * Send HTTP POST request to {@link #getEndpointUrl(), imports impex
	 *
	 * @param file
	 *            file to be imported
	 * @return import status message
	 * @throws IOException
	 * @throws HttpResponseException
	 */
	private String postImpex(final IFile file) throws HttpResponseException, IOException {
		final Map<String, String> parameters = new HashMap<>();
		final HttpPost postRequest = new HttpPost(getEndpointUrl() + ImpexImport.IMPEX_IMPORT_PATH);
		final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(getTimeout()).build();
		
		parameters.put(ImpexImport.Parameters.ENCODING, getEncoding());
		parameters.put(ImpexImport.Parameters.SCRIPT_CONTENT, getContentOfFile(file));
		parameters.put(ImpexImport.Parameters.MAX_THREADS, ImpexImport.Parameters.MAX_THREADS_VALUE);
		parameters.put(ImpexImport.Parameters.VALIDATION_ENUM, ImpexImport.Parameters.VALIDATION_ENUM_VALUE);
		
		postRequest.setConfig(requestConfig);
		postRequest.addHeader(getxCsrfToken(), getCsrfToken());
		postRequest.setEntity(new UrlEncodedFormEntity(createParametersList(parameters)));
		
		final HttpResponse response = getHttpClient().execute(postRequest, getContext());
		final String responseBody = new BasicResponseHandler().handleResponse(response);
		
		return getImportStatus(responseBody);
	}

	/**
	 * Extract impex import response from the parameter
	 * 
	 * @param responseBody HTML document
	 * @return response message
	 */
	private String getImportStatus(final String responseBody) {
		final Document document = Jsoup.parse(responseBody);
		String importStatus = document.select(Meta.IMPEX_IMPORT_RESULT_SPAN).attr(Meta.IMPEX_IMPORT_RESULT_SPAN_ATTR);
		if (importStatus.equals(Messages.IMPORT_ERROR)) {
			ConsoleUtils.printMessage(document.select(Meta.IMPEX_IMPORT_ERROR_RESULT_ELEMENT).text());
			importStatus += Messages.CHECK_CONSOLE;
		} else if (importStatus.isEmpty()) {
			importStatus = Messages.EMPTY_FILE_ERROR;
		}
		return importStatus;
	}
}
