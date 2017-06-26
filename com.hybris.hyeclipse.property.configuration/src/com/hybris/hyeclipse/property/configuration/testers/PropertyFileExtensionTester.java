package com.hybris.hyeclipse.property.configuration.testers;

import com.hybris.hyeclipse.commons.testers.AbstractFilePropertyTester;

/**
 * Test whether the file has {@link PropertyFileExtensionTester#PROPERTY_FILE_EXTENSION} extension.
 */
public class PropertyFileExtensionTester extends AbstractFilePropertyTester  {

	/**
	 * Extension of a properties file.
	 */
	private static final String PROPERTY_FILE_EXTENSION = "properties";
	
	/**
	 * Property to check 
	 */
	private static final String PROPERTY_NAME = "isItPropertyFile";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean test(final Object receiver,final String property,final Object[] args,final Object expectedValue) { 
		return PROPERTY_NAME.equals(property) && testSelectedFileByExtension(receiver, PROPERTY_FILE_EXTENSION);
	}
}
