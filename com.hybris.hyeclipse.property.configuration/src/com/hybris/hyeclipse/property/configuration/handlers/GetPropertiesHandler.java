package com.hybris.hyeclipse.property.configuration.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.hybris.hyeclipse.property.configuration.managers.PropertyManager;
import com.hybris.hyeclipse.utils.ConsoleUtils;
import com.hybris.hyeclipse.utils.Constatns;

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
		                .map(property -> property.getKey() + Constatns.EQUALS_CHARCTER + property.getValue())
		                .forEach(ConsoleUtils::printMessage);

		return null;
	}

}
