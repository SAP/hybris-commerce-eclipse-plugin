package com.hybris.hyeclipse.commons.handlers;

import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.commons.utils.EclipseFileUtils;

/**
 * Abstract handler for selection of 1..n file(s) or text selection in the editor
 */
public abstract class AbstractSelectionHandler extends AbstractHandler {
	
	/**
	 * Eclipse context debug value. 
	 * 
	 * I've not found better way to find out which menu has been clicked. 
	 */
	private static final String ECLIPSE_MENU_CONTEXT_KEY = "debugString";
	private static final String TEXT_EDITOR_MENU_VALUE = "popup:#TextEditorContext";
	private static final String PROJECT_EXPLORER_MENU_VALUE = "popup:org.eclipse.ui.navigator.ProjectExplorer#PopupMenu";
	
	/**
	 * {@inheritDoc} 
	 */
	@SuppressWarnings("restriction")
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ExpressionContext context = (ExpressionContext) event.getApplicationContext();
		final String clickedMenuKey = context.getVariable(ECLIPSE_MENU_CONTEXT_KEY).toString();
		
		if( PROJECT_EXPLORER_MENU_VALUE.equals(clickedMenuKey) ) {
			handle(EclipseFileUtils.getSelectedFiles(HandlerUtil.getCurrentSelection(event)));
		} else if( TEXT_EDITOR_MENU_VALUE.equals(clickedMenuKey) ) {
			handle(EclipseFileUtils.getCurrentTextSelection().get());
		}
								
		return null; // intended.
	}
	
	/**
	 * Handle selected 1..n selected file(s)
	 * 
	 * @param files selected files
	 */
	protected abstract void handle(final Set<IFile> files);
	
	/**
	 * Handle selected text in editor
	 * 
	 * @param selectedText current text selection
	 */
	protected abstract void handle(final TextSelection selectedText);
}
