package com.hybris.hyeclipse.script.executor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.hac.utils.EclipseFileUtils;
import com.hybris.hyeclipse.script.executor.managers.ScriptExecutorManager;

/**
 * Class handles script import to the hAC.
 */
public class ScriptImportHandler  extends AbstractHandler {
	
	private ScriptExecutorManager scriptExecutorManager = new ScriptExecutorManager();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IFile scriptFile = EclipseFileUtils.getSelectedFile(HandlerUtil.getCurrentSelection(event));
		scriptExecutorManager.importScript(scriptFile);
		return null;
	}
	
	
}
