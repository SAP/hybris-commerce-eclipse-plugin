package com.hybris.hyeclipse.property.configuration.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jsoup.helper.StringUtil;
import com.hybris.hyeclipse.utils.Constatns;
import com.hybris.hyeclipse.utils.EclipseFileUtils;

/**
 * Abstract property handler
 */
public abstract class AbstractPropertyHandler extends AbstractHandler {

	/**
	 * Active shell
	 */
	private Shell activeShell;
	
	/**
	 * Pattern to match property string (key=value)
	 */
	protected static Pattern propertyPattern = Pattern.compile("(.*)=(.*)?");

	private static final String ERROR_DIALOG_TITLE = "Invalid property";
	private static final String INVALID_PROPERTY = "Error ocurred due to following lines: " + Constatns.NEW_LINE;
	
	/**
	 * {@inheritDoc}
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final String selectedText = EclipseFileUtils.getSelectedFileText();

		setActiveShell(HandlerUtil.getActiveShell(event));
		if (StringUtil.isBlank(selectedText)) {
			execute(extractPropertiesFromString(EclipseFileUtils
					.getContentOfFiles(
							EclipseFileUtils.getSelectedFiles(
									HandlerUtil.getCurrentSelection(event)
									)
							)
					)
					);
		} else {
			execute(extractPropertiesFromString(selectedText));
		}
		
		return null; 
	}

	/**
	 * Execute action of a handler.
	 */
	protected abstract void execute(final Map<String, String> properties);

	/**
	 * Validate properties
	 * 
	 * @param properties
	 *            list of properties to validate
	 * @return true if all properties are valid, false otherwise
	 */
	protected boolean validate(final List<String> properties) {
		boolean isValid = true;
		final List<String> invalidProperties = new ArrayList<String>();

		for (final String property : properties) {
			if (!StringUtil.isBlank(property) && !propertyPattern.matcher(property).find()) {
				isValid = false;

				invalidProperties.add(property);
			}
		}

		if( !isValid ) {
			final StringBuilder errorMessageBuilder = new StringBuilder(INVALID_PROPERTY);
			invalidProperties.forEach(
					invalidProperty -> errorMessageBuilder
						.append(invalidProperty)
						.append(Constatns.NEW_LINE)
						);
			
			MessageDialog.openError(getActiveShell(), ERROR_DIALOG_TITLE, errorMessageBuilder.toString());
		}

		return isValid;
	}

	/**
	 * Extracts properties from String to the Map of strings
	 * 
	 * @param propertiesString
	 *            string of properties
	 * @return map of properties, with key, value.
	 */
	protected Map<String, String> extractPropertiesFromString(final String propertiesString) {
		final List<String> propertiesList = Arrays.asList(propertiesString.split(Constatns.NEW_LINE)).stream()
				.filter(item -> !StringUtil.isBlank(item)).collect(Collectors.toList());

		final Map<String, String> propertiesMap = new HashMap<>();
		if (validate(propertiesList)) {
			for( final String property : propertiesList ) {
				final String[] keyValueArray = property.split(Constatns.EQUALS_CHARCTER);
				propertiesMap.put(keyValueArray[0], keyValueArray[1]);
			}
		}

		return propertiesMap;
	}

	protected Shell getActiveShell() {
		return activeShell;
	}

	protected void setActiveShell(Shell activeShell) {
		this.activeShell = activeShell;
	}
}
