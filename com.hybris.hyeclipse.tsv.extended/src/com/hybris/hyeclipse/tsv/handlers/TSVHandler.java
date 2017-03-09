package com.hybris.hyeclipse.tsv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.tsv.wizards.RunTSVWizard;

/**
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class TSVHandler extends AbstractHandler {
	
	public TSVHandler() {}

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		
		Shell activeShell = HandlerUtil.getActiveShell(event);
		IWizard wizard = new RunTSVWizard();
		
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
		
		return null;
	}

}
