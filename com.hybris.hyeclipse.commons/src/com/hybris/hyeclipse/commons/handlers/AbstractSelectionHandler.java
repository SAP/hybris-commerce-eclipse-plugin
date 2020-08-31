/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
			handle(EclipseFileUtils.getCurrentTextSelection().orElseThrow(() -> new ExecutionException("missing text selection")));
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
