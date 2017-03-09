package com.hybris.impexformatter.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.impexformatter.wizards.NewImpexWizard;

public class NewImpexWizardHandler extends AbstractHandler {

	@Override
	public Object execute( ExecutionEvent event ) throws ExecutionException
	{
		Shell activeShell = HandlerUtil.getActiveShell(event);
		NewImpexWizard newWizard = new NewImpexWizard();
		newWizard.setCurrentSelection(HandlerUtil.getCurrentSelection(event));
		
		IWizard wizard = (IWizard) newWizard;
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
		return null;
	}

}
