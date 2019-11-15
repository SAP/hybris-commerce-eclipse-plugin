package com.hybris.yps.hyeclipse.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.ytypesystem.Activator;
import com.hybris.yps.hyeclipse.utils.ProjectSourceUtil;

public class DetachSourcesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell activeShell = HandlerUtil.getActiveShell(event);
		IRunnableWithProgress runner = ProjectSourceUtil.getRunner();

		try {
			new ProgressMonitorDialog(activeShell).run(true, false, runner);

		} catch (InvocationTargetException e) {
			Activator.logError("Error detaching sources", e);
			MessageDialog.openError(activeShell, "Error detaching sources", e.getMessage());
		} catch (InterruptedException e) {
			Activator.logError("Error detaching sources", e);
			Thread.currentThread().interrupt();
		}
		return null;
	}

}
