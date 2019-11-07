package com.hybris.hyeclipse.script.executor.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.commons.utils.EclipseFileUtils;
import com.hybris.hyeclipse.script.executor.managers.ScriptExecutorManager;

/**
 * Class handles script import to the hAC.
 */
public class ScriptCommitHandler  extends AbstractHandler {
	
	private ScriptExecutorManager scriptExecutorManager = new ScriptExecutorManager();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IFile scriptFile = EclipseFileUtils.getSelectedFile(HandlerUtil.getCurrentSelection(event));
		scriptExecutorManager.commitScript(scriptFile);
		return null;
	}
	
	
}
