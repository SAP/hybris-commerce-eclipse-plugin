package com.hybris.hyeclipse.property.configuration.handlers;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;

import com.hybris.hyeclipse.property.configuration.managers.PropertyManager;

/**
 * Handler for save property command
 */
public class SavePropertyHandler extends AbstractPropertyHandler {

	private static final String DIALOG_TILE = "Upload platform config";
	private static final String DIALOG_MESSAGE = "Are you sure, you want to upload selected properties to the platform?";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void execute(final Map<String, String> properties) {
		if( MessageDialog.openConfirm(getActiveShell(), DIALOG_TILE, DIALOG_MESSAGE) ) {
			final PropertyManager manager = new PropertyManager();
			
			if(manager.checkHacHealth()) {
				properties.forEach((key,value) -> manager.saveProperty(key, value)); 
			}
		}
	} 
}
