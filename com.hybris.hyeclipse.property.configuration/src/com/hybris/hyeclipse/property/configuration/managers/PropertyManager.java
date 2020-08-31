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
package com.hybris.hyeclipse.property.configuration.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hybris.hyeclipse.hac.manager.AbstractHACCommunicationManager;

/**
 * Class to communicate with HAC to get, add, update and remove property.
 */
public class PropertyManager extends AbstractHACCommunicationManager {

	/**
	 * Property API constants
	 */
	private interface PropertyApi {

		interface Urls {
			static final String GET = "/platform/config";
			static final String STORE = "/platform/configstore/";
			static final String REMOVE = "/platform/configdelete/";
			static final String SAVE = "/platform/config/valuechanged/";
		}

		interface Parameters {
			static final String KEY = "key";
			static final String VALUE = "val";
		}

		interface HTML {
			static final String PARAMETER_NAME_ATTR_NAME = "name";
			static final String PARAMETER_VALUE_ATTR_NAME = "value";
			static final String PARAMETER_INPUT_CLASS = "configValue";
		}
	}

	static final String PROPERTY_NOT_FOUND = "Property not found";

	/**
	 * Get properties from HAC and return it as a list.
	 * 
	 * @return list of HAC properties
	 */
	public Map<String, String> getProperties() {
		final String hacParametersPageHTML = sendAuthenticatedGetRequest(PropertyApi.Urls.GET);
		final Document htmlDocument = Jsoup.parse(hacParametersPageHTML);
		final Elements elements = htmlDocument.getElementsByClass(PropertyApi.HTML.PARAMETER_INPUT_CLASS);

		Map<String, String> propertiesMap = elements.stream()
		                .collect(Collectors.toMap(
		                                (Element element) -> element.attr(PropertyApi.HTML.PARAMETER_NAME_ATTR_NAME),
		                                (Element element) -> element.attr(PropertyApi.HTML.PARAMETER_VALUE_ATTR_NAME)));

		return propertiesMap;
	}

	/**
	 * Get property value from platform
	 * 
	 * @param propertyName
	 *            name of property to get
	 * @return parameter value if present, {@link #PROPERTY_NOT_FOUND} otherwise
	 */
	public String getPropertyValue(final String propertyName) {
		final Optional<Entry<String, String>> propertyValue = getProperties().entrySet()
									.stream()
					                .filter(property -> property.getKey().equals(propertyName))
					                .findFirst();

		if (propertyValue.isPresent()) {
			return propertyValue.get().getValue();
		} else {
			return PROPERTY_NOT_FOUND;
		}
	}

	/**
	 * Save parameter to the platform
	 * 
	 * @param propertyName
	 *            parameter name to add
	 * @param propertyValue
	 *            parameter value to add
	 */
	public void saveProperty(final String propertyName, final String propertyValue) {
		final Map<String, String> parameters = new HashMap<>();

		parameters.put(PropertyApi.Parameters.KEY, propertyName);
		parameters.put(PropertyApi.Parameters.VALUE, propertyValue);

		sendAuthenticatedPostRequest(PropertyApi.Urls.SAVE, parameters);
		sendAuthenticatedPostRequest(PropertyApi.Urls.STORE, parameters);
	}

	/**
	 * Removes property property from platform via HAC
	 * 
	 * @param propertyName
	 *            property name to remove
	 */
	public void removeProperty(final String propertyName) {
		final Map<String, String> parameters = new HashMap<>();

		parameters.put(PropertyApi.Parameters.KEY, propertyName);
		sendAuthenticatedPostRequest(PropertyApi.Urls.REMOVE, parameters);
	}
}
