package com.hybris.hyeclipse.property.configuration.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.widgets.Shell;

import com.hybris.hyeclipse.commons.handlers.AbstractSelectionHandler;
import com.hybris.hyeclipse.commons.utils.ConsoleUtils;
import com.hybris.hyeclipse.commons.utils.CharactersConstants;
import com.hybris.hyeclipse.commons.utils.EclipseFileUtils;

/**
 * Abstract property handler
 */
public abstract class AbstractPropertyHandler extends AbstractSelectionHandler {

	private static final String PROCESSING_FILE_MESSAGE_FORMAT = "Processing file: %1$s";
	private static final String INVALID_PROPERTY_ERROR_MESSAGE_FORMAT = "Invalid property '%1$s' in line %2$d";
	
	/**
	 * Pattern to match property string (key=value)
	 */
	protected static Pattern PROPERTY_PATTERN = Pattern.compile("(.*)=(.*)?");

	/**
	 * Active shell
	 */
	private Shell activeShell;

	/**
	 * Execute action of a handler.
	 */
	protected abstract void execute(final Map<String, String> properties);

	/**
	 * {@inheritDoc}
	 */
	protected void handle(final Set<IFile> files) {
		final Map<String, String> propertiesMap = new HashMap<>();
		for (IFile file : files) {
			ConsoleUtils.printMessage(String.format(PROCESSING_FILE_MESSAGE_FORMAT, file.getName()));
			propertiesMap.putAll(extractProperties(EclipseFileUtils.getContentOfFile(file), 1));
		}

		execute(propertiesMap);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void handle(final TextSelection textSelection) {
		execute(extractProperties(textSelection.getText(), textSelection.getStartLine() + 1));
	}

	/**
	 * Extract properties from file
	 * 
	 * @param properties
	 *            content of a file
	 * @param startLine
	 *            start line of the properties from file
	 * 
	 * @return map of properties and their values
	 */
	protected Map<String, String> extractProperties(final String properties, final int startLine) {
		final Map<String, String> propertiesMap = new HashMap<>();
		final List<String> propertyList = Arrays.asList(properties.split(CharactersConstants.NEW_LINE));

		int line = startLine;
		for (String property : propertyList) {
			propertiesMap.putAll(extractPropertiesFromString(property, line++));
		}

		return propertiesMap;
	}

	/**
	 * Extract property key and value from string
	 * 
	 * @param property
	 *            property string to extract
	 * @param line
	 *            line of property string in file
	 * 
	 * @return extracted property if present.
	 */
	protected Map<String, String> extractPropertiesFromString(final String property, final int line) {
		final Map<String, String> propertyMap = new HashMap<>();

		if (StringUtils.isNotBlank(property) && validate(property, line)) {
			final String[] propertyKeyValue = property.split(CharactersConstants.EQUALS_CHARCTER);

			propertyMap.put(propertyKeyValue[0],
			                (propertyKeyValue.length > 1) ? propertyKeyValue[1] : CharactersConstants.EMPTY_STRING);
		}

		return propertyMap;
	}

	/**
	 * Validate particular property
	 * 
	 * @param property
	 *            property to validate
	 * @param line
	 *            property line in the file
	 * 
	 * @return true if property is valid, false otherwise
	 */
	protected boolean validate(final String property, final int line) {
		if (!PROPERTY_PATTERN.matcher(property).find()) {
			ConsoleUtils.printError(String.format(INVALID_PROPERTY_ERROR_MESSAGE_FORMAT, property, line));
			return false;
		}

		return true;
	}

	protected Shell getActiveShell() {
		return activeShell;
	}

	protected void setActiveShell(Shell activeShell) {
		this.activeShell = activeShell;
	}
}
