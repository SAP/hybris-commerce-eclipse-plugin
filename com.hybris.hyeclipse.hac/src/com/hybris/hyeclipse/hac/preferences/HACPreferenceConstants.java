package com.hybris.hyeclipse.hac.preferences;

import java.util.regex.Pattern;

public class HACPreferenceConstants {
	// preferences
	public static final String P_HOSTNAME_URL = "endpointHostname";
	public static final String P_USERNAME = "username";
	public static final String P_PASSWORD = "password";
	public static final String P_TIMEOUT = "timeout";

	// values
	public static final String V_HOSTNAME_URL = "https://localhost:9002";
	public static final String V_USERNAME = "admin";
	public static final String V_PASSWORD = "nimda";
	public static final int V_TIMEOUT = 60;
	
	/**
	 * Regex of URI allowed characters
	 */
	public static final String URI_HOST_REGEXP = "[a-zA-Z0-9\\.\\-]+";
	public static final String HTTP_HTTPS_REGEXP = "https?://";
	public static final String PORT_AND_PATH_REGEX = "(:(\\d+))?(/(.*))?";
	
	/**
	 * HAC URL validation pattern
	 */
	public static final Pattern URL_PATTERN = Pattern.compile(HTTP_HTTPS_REGEXP + URI_HOST_REGEXP + PORT_AND_PATH_REGEX);
	
	/**
	 * Compiled regular expression pattern to match host name from URL.
	 */
	public static final Pattern HOST_REGEXP_PATTERN = Pattern.compile("//(" + URI_HOST_REGEXP + ")");
}
