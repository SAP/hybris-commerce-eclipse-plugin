package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.yps.hyeclipse.wizards.AttachSourcesWizard;

/**
 * Handler that will start the Attach Sources Wizard from the menu bar.
 * 
 * @author mheuer
 *
 */
public class AttachSourcesHandler extends AbstractHandler
{
	@Override
	public Object execute( ExecutionEvent event ) throws ExecutionException
	{
		Shell activeShell = HandlerUtil.getActiveShell(event);
		IWizard wizard = new AttachSourcesWizard();
		
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
		
		return null;
	}
}
