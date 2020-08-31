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
package com.hybris.hyeclipse.property.configuration.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.hybris.hyeclipse.commons.utils.ConsoleUtils;
import com.hybris.hyeclipse.commons.utils.CharactersConstants;
import com.hybris.hyeclipse.property.configuration.managers.PropertyManager;

/**
 * Print properties to the console handler.
 */
public class GetPropertiesHandler extends AbstractHandler {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final PropertyManager propertyManager = new PropertyManager();

		propertyManager.getProperties().entrySet().stream()
		                .map(property -> property.getKey() + CharactersConstants.EQUALS_CHARCTER + property.getValue())
		                .forEach(ConsoleUtils::printMessage);

		return null;
	}

}
