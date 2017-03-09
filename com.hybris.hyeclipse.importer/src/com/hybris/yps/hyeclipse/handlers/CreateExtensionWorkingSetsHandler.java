package com.hybris.yps.hyeclipse.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.utils.WorkingSetsUtils;
import com.hybris.yps.hyeclipse.wizards.CreateWorkingSetWizard;

public class CreateExtensionWorkingSetsHandler extends AbstractHandler {

	boolean commentsFound = false;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell activeShell = HandlerUtil.getActiveShell(event);
		IWizard wizard = new CreateWorkingSetWizard();
		
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
		
		return null;
		
//		IRunnableWithProgress op = new IRunnableWithProgress() {
//
//			@Override
//			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//				setCommentsFound(WorkingSetsUtils.organizeWorkingSetsFromLocalExtensions(monitor));
//			}
//		};
//
//		try {
//			new ProgressMonitorDialog(new Shell()).run(true, false, op);
//
//			if (!commentsFound) {
//				Shell shell = new Shell();
//				MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
//				dialog.setText("No comments found in localextensions.xml");
//				dialog.setMessage(
//						"Working Sets are created based on the comments in your localextensions.xml (see accelerator for examples).\n\n Please add some comments and try again.");
//				dialog.open();
//			}
//		} catch (InvocationTargetException e) {
//			Activator.logError("InvocationTargetException", e);
//		} catch (InterruptedException e) {
//			Activator.logError("InterruptedException", e);
//		}
//		return null;
	}

	private void setCommentsFound(boolean b) {
		commentsFound = b;
	}

}
