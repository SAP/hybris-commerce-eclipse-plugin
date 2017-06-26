package com.hybris.hyeclipse.commons.handlers;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.commons.utils.EclipseFileUtils;

/**
 * Abstract handler for selection of 1..n file(s) or text selection in the editor
 */
public abstract class AbstractSelectionHandler extends AbstractHandler {
	
	/**
	 * {@inheritDoc} 
	 */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Set<IFile> files = EclipseFileUtils.getSelectedFiles(HandlerUtil.getCurrentSelection(event));
		final Optional<TextSelection> textSelection = EclipseFileUtils.getCurrentTextSelection();
		
		if( files.size() == 1 && textSelection.isPresent() && textSelection.get().getStartLine() > 0 ) {
			handle(textSelection.get());
		} else {
			handle(files);
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
