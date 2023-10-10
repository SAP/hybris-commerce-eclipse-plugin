/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.hac.manager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;

import com.hybris.hyeclipse.commons.utils.ConsoleUtils;
import com.hybris.hyeclipse.hac.Activator;
import com.hybris.hyeclipse.hac.preferences.HACPreferenceConstants;

/**
 * Abstract class to communicate with hAC web page.
 */
public abstract class AbstractHACCommunicationManager {

	/**
	 * HAC authentication properties
	 */
	protected final class Authentication {
		static final String LOCATION_ERROR = "error";
		static final String LOCATION_HEADER = "Location";

		final class Parameters {
			static final String USERNAME = "j_username";
			static final String PASSWORD = "j_password";
			
			private Parameters() {}
		}

		final class Path {
			static final String LOGIN = "/j_spring_security_check";
			static final String LOGOUT = "/j_spring_security_logout";
			
			private Path() {}
		}
	}

	/**
	 * HAC communication meta data properties
	 */
	protected final class Meta {
		static final String X_CSRF_TOKEN = "X-CSRF-Token";
		static final String CSRF_META_TAG_CONTENT = "content";
		static final String CSRF_META_TAG = "meta[name='_csrf']";

		static final int NOT_FOUND_STATUS_CODE = 404;
		
		private Meta() {}
	}

	/**
	 * Error messages for communication with HAC
	 */
	protected final class ErrorMessage {
		static final String SERVER_RESPONSE = "Server response: ";
		static final String INVALID_HAC_URL = "HAC URL is invalid";
		static final String WRONG_CREDENTIALS = " Wrong login credentials.";
		static final String CANNOT_CREATE_SSL_SOCKET = "Cannot create SSL socket";
		static final String UNKNOWN_HOST_EXCEPTION_MESSAGE_FORMAT = "Host: $1%s is unreachable";
		static final String CSRF_RESPONSE_CANNOT_BE_BLANK = "HAC authentication response cannot be empty.";
		static final String CSRF_TOKEN_CANNOT_BE_OBTAINED = "Cannot obtain CSRF authentication token from HAC.";
		
		private ErrorMessage() {}
	}

	/**
	 * Maximum time in seconds to wait for response
	 */
	private int timeout;

	/**
	 * hAC user name
	 */
	private String username;

	/**
	 * hAC user's password
	 */
	private String password;

	/**
	 * csrf token received from hAC page
	 */
	private String csrfToken;

	/**
	 * hAC URL
	 */
	private String endpointUrl;

	/* HTTP communication properties */
	private HttpClientContext context;
	private final HttpClient httpClient;

	protected AbstractHACCommunicationManager() {
		httpClient = getSSLAcceptingClient();
		context = HttpClientContext.create();
		context.setCookieStore(new BasicCookieStore());
	}
	
	/**
	 * Check whether HAC is up
	 * 
	 * @return true if HAC is online, false otherwise
	 */
	public boolean checkHacHealth() {
		final String response = sendAuthenticatedGetRequest(StringUtils.EMPTY);
		return !StringUtil.isBlank(response);
	}

	/**
	 * Creates {@link HttpClient} that trusts any SSL certificate
	 *
	 * @return prepared HTTP client
	 */
	protected HttpClient getSSLAcceptingClient() {
		final TrustStrategy trustAllStrategy = (final X509Certificate[] chain, final String authType) -> true;
		try {
			final SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, trustAllStrategy).build();

			sslContext.init(null, getTrustManager(), new SecureRandom());
			final SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
					new NoopHostnameVerifier());

			return HttpClients.custom().setSSLSocketFactory(connectionSocketFactory).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException error) {
			ConsoleUtils.printError(error.getMessage());
			throw new IllegalStateException(ErrorMessage.CANNOT_CREATE_SSL_SOCKET, error);
		}
	}

	/**
	 * Creates {@link TrustManager} that trusts any SSL certificate
	 *
	 * @return prepared TrustManager
	 */
	protected TrustManager[] getTrustManager() {
		return new TrustManager[] {};
	}

	/**
	 * Send HTTP GET request to {@link #endpointUrl}, updates {@link #csrfToken}
	 * token
	 *
	 * @return true if {@link #endpointUrl} is accessible
	 * @throws IOException
	 * @throws AuthenticationException
	 */
	protected void fetchCsrfTokenFromHac() throws IOException, AuthenticationException {
		final HttpGet getRequest = new HttpGet(getEndpointUrl());

		try {
			final HttpResponse response = httpClient.execute(getRequest, getContext());
			final String responseString = new BasicResponseHandler().handleResponse(response);
			csrfToken = getCsrfToken(responseString);

			if (StringUtil.isBlank(csrfToken)) {
				throw new AuthenticationException(ErrorMessage.CSRF_TOKEN_CANNOT_BE_OBTAINED);
			}
		} catch (UnknownHostException error) {
			final String errorMessage = error.getMessage();
			final Matcher matcher = HACPreferenceConstants.HOST_REGEXP_PATTERN.matcher(getEndpointUrl());

			if (matcher.find() && matcher.group(1).equals(errorMessage)) {
				throw new UnknownHostException(
						String.format(ErrorMessage.UNKNOWN_HOST_EXCEPTION_MESSAGE_FORMAT, matcher.group(1)));
			}
			throw error;
		}
	}

	/**
	 * Retrieves csrf token from response body
	 *
	 * @param responseBody
	 *            response body of GET method
	 * @return csrf token
	 * @throws AuthenticationException
	 */
	protected String getCsrfToken(String responseBody) throws AuthenticationException {
		if (StringUtil.isBlank(responseBody)) {
			throw new AuthenticationException(ErrorMessage.CSRF_RESPONSE_CANNOT_BE_BLANK);
		}

		final Document document = Jsoup.parse(responseBody);
		return document.select(Meta.CSRF_META_TAG).attr(Meta.CSRF_META_TAG_CONTENT);
	}

	/**
	 * Send HTTP POST request to {@link #endpointUrl}, logins to HAC
	 *
	 * @throws IOException
	 */
	protected void loginToHac() throws IOException {
		final HttpPost postRequest = new HttpPost(endpointUrl + Authentication.Path.LOGIN);
		final Map<String, String> parameters = new HashMap<>();

		parameters.put(Authentication.Parameters.USERNAME, username);
		parameters.put(Authentication.Parameters.PASSWORD, password);
		postRequest.addHeader(Meta.X_CSRF_TOKEN, csrfToken);
		postRequest.setEntity(new UrlEncodedFormEntity(createParametersList(parameters)));

		final HttpResponse response = httpClient.execute(postRequest, context);
		final Header[] locationHeaders = response.getHeaders(Authentication.LOCATION_HEADER);

		if (Meta.NOT_FOUND_STATUS_CODE == response.getStatusLine().getStatusCode()) {
			throw new IOException(ErrorMessage.INVALID_HAC_URL);
		} else if (locationHeaders.length > 0
				&& locationHeaders[0].getValue().contains(Authentication.LOCATION_ERROR)) {
			throw new IOException(ErrorMessage.WRONG_CREDENTIALS);
		}
	}

	/**
	 * Send HTTP POST request to {@link #endpointUrl}, logouts from HAC
	 *
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	protected void logoutFromHac() throws IOException {
		final HttpPost post = new HttpPost(endpointUrl + Authentication.Path.LOGOUT);
		post.addHeader(Meta.X_CSRF_TOKEN, csrfToken);
		httpClient.execute(post, context);
	}

	/**
	 * Creates a parameters list from provided map
	 * 
	 * @param parametersMap
	 *            parameters map to convert
	 * @return List of parameters
	 */
	protected List<NameValuePair> createParametersList(final Map<String, String> parametersMap) {
		return parametersMap.entrySet()
						.stream()
						.map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
						.collect(Collectors.toList());
	}

	/**
	 * Updates the {@link #endpointUrl}, {@link #timeout}, {@link #username} and
	 * {@link #password} basing on the data provided to preference page
	 */
	protected void updateLoginVariables() {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		username = store.getString(HACPreferenceConstants.P_USERNAME);
		password = store.getString(HACPreferenceConstants.P_PASSWORD);
		timeout = store.getInt(HACPreferenceConstants.P_TIMEOUT) * 1000;
		endpointUrl = store.getString(HACPreferenceConstants.P_HOSTNAME_URL);
	}

	/**
	 * Send post request to the {@link #getEndpointUrl()} with suffix as a url
	 * parameter.
	 * 
	 * @param url
	 *            suffix to the {@link #getEndpointUrl()}.
	 * @param parameters
	 *            map of parameters that will be attached to the request
	 * @return response of the request
	 * @throws IOException
	 */
	protected String sendPostRequest(final String url, final Map<String, String> parameters)
			throws IOException {
		final HttpPost postRequest = new HttpPost(getEndpointUrl() + url);
		final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(getTimeout()).build();

		postRequest.setConfig(requestConfig);
		postRequest.addHeader(getxCsrfToken(), getCsrfToken());
		postRequest.setEntity(new UrlEncodedFormEntity(createParametersList(parameters)));

		final HttpResponse response = getHttpClient().execute(postRequest, getContext());
		return new BasicResponseHandler().handleResponse(response);
	}

	/**
	 * Send get request to the {@link #getEndpointUrl()} with suffx as a URL
	 * parameter.
	 * 
	 * @param url
	 *            suffix to the {@link #getEndpointUrl()}
	 * @return response of the request
	 * @throws IOException
	 */
	protected String sendGetRequest(final String url) throws IOException {
		final HttpGet getRequest = new HttpGet(getEndpointUrl() + url);
		final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(getTimeout()).build();

		getRequest.setConfig(requestConfig);
		getRequest.addHeader(getxCsrfToken(), getCsrfToken());

		final HttpResponse response = getHttpClient().execute(getRequest, getContext());
		return new BasicResponseHandler().handleResponse(response);
	}

	/**
	 * Send post request to the {@link #getEndpointUrl()} with suffix as a url
	 * parameter.
	 * 
	 * @param url
	 *            suffix to the {@link #getEndpointUrl()}.
	 * @param parameters
	 *            map of parameters that will be attached to the request
	 * @return response of the request
	 */
	protected String sendAuthenticatedPostRequest(final String url, final Map<String, String> parameters) {
		String response = null;
		updateLoginVariables();

		try {
			fetchCsrfTokenFromHac();
			loginToHac();
			fetchCsrfTokenFromHac();
			response = sendPostRequest(url, parameters);
			logoutFromHac();
		} catch (ConnectException | IllegalArgumentException error) {
			ConsoleUtils.printError(error.getMessage());
		} catch (final IOException | AuthenticationException error) {
			ConsoleUtils.printError(ErrorMessage.SERVER_RESPONSE + error.getMessage());
		}

		return response;
	}

	/**
	 * Send get request to the {@link #getEndpointUrl()} with suffx as a URL
	 * parameter.
	 * 
	 * @param url
	 *            suffix to the {@link #getEndpointUrl()}
	 * @return response of the request
	 */
	protected String sendAuthenticatedGetRequest(final String url) {
		String response = null;
		updateLoginVariables();

		try {
			fetchCsrfTokenFromHac();
			loginToHac();
			fetchCsrfTokenFromHac();
			response = sendGetRequest(url);
			logoutFromHac();
		} catch (ConnectException | IllegalArgumentException error) {
			ConsoleUtils.printError(error.getMessage());
		} catch (final IOException | AuthenticationException error) {
			ConsoleUtils.printError(ErrorMessage.SERVER_RESPONSE + error.getMessage());
		}

		return response;
	}

	protected static String getxCsrfToken() {
		return Meta.X_CSRF_TOKEN;
	}

	protected HttpClient getHttpClient() {
		return httpClient;
	}

	protected HttpClientContext getContext() {
		return context;
	}

	protected String getEndpointUrl() {
		return endpointUrl;
	}

	protected String getUsername() {
		return username;
	}

	protected String getPassword() {
		return password;
	}

	protected String getCsrfToken() {
		return csrfToken;
	}

	protected int getTimeout() {
		return timeout;
	}
}
