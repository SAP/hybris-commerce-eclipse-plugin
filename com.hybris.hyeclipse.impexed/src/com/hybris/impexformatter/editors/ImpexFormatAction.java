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
package com.hybris.impexformatter.editors;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * Action responsible for formatting impex file
 * @author SAP
 *
 */
public class ImpexFormatAction implements IEditorActionDelegate {

	private IEditorPart editorPart = null;
	
	@Override
	public void run(IAction arg0) {
		if(editorPart != null) {
			ImpexPageEditor.formatText(editorPart);
		}
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		
	}

	@Override
	public void setActiveEditor(IAction arg0, IEditorPart arg1) {
		this.editorPart = arg1;
	}

}
