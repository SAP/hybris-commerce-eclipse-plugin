package com.hybris.hyeclipse.extgen.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.extgen.wizards.NewExtensionWizard;

/**
 * Handler for {@link NewExtensionWizard}
 */
public class ExtensionWizardHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell activeShell = HandlerUtil.getActiveShell(event);
		NewExtensionWizard newWizard = new NewExtensionWizard();
		newWizard.setCurrentSelection(HandlerUtil.getCurrentSelection(event));

		WizardDialog dialog = new WizardDialog(activeShell, newWizard);
		dialog.open();
		return null;
	}
}
