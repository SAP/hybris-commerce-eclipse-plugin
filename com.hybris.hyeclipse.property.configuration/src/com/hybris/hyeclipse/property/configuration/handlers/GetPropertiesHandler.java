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
