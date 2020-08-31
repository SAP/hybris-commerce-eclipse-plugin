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
