package com.hybris.impexformatter.utils;

public class StringHelper {
	
	/**
	 * checks if in <pre>typecode</pre> is possible to find <pre>prefix</pre>. Case insensitive.
	 * @param typeCode that is main string
	 * @param prefix check if given prefix exists
	 * @return true if there is a match case insensitive and is less than main string.
	 */
	public static boolean findMatches(String typeCode, String prefix) {
		String tcUp = typeCode.toUpperCase();
		String cpUp = prefix.toUpperCase();
		return tcUp.startsWith(cpUp) && prefix.length() < typeCode.length();
	}


}
