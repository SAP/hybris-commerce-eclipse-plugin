package com.hybris.hyeclipse.property.configuration.handlers;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;

import com.hybris.hyeclipse.property.configuration.managers.PropertyManager;

/**
 * Handler for removing property command  
 */
public class RemovePropertyHandler extends AbstractPropertyHandler {

	private static final String DIALOG_TILE = "Remove platform config";
	private static final String DIALOG_MESSAGE = "Are you sure, you want to remove properties from the platform?";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void execute(Map<String, String> properties) {
		if( MessageDialog.openConfirm(getActiveShell(), DIALOG_TILE, DIALOG_MESSAGE) ) {
			final PropertyManager manager = new PropertyManager();

			if(manager.checkHacHealth()) {
				properties.entrySet().stream().forEach(
							property -> manager.removeProperty(property.getKey())
						);
			}
		}
	}
	
}
