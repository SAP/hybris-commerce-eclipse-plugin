package com.hybris.impexformatter.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * Action responsible for formatting impex file
 * @author I303764
 *
 */
public class ImpexFormatAction implements IEditorActionDelegate {

	@Override
	public void run(IAction arg0) {
		ImpexPageEditor.formatText();
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		
	}

	@Override
	public void setActiveEditor(IAction arg0, IEditorPart arg1) {
		
	}

}
