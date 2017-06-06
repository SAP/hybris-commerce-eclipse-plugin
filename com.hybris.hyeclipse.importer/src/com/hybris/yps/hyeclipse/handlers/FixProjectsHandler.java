package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.yps.hyeclipse.wizards.SynchronizePlatformWizard;

public class FixProjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {

		Shell activeShell = HandlerUtil.getActiveShell(event);
		IWizard wizard = new SynchronizePlatformWizard();
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();	
		return null;
	}
}
