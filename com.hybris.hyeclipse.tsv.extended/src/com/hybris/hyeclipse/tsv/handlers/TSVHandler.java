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
package com.hybris.hyeclipse.tsv.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.tsv.wizards.RunTSVWizard;

/**
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class TSVHandler extends AbstractHandler {
	
	public TSVHandler() {}

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		
		Shell activeShell = HandlerUtil.getActiveShell(event);
		IWizard wizard = new RunTSVWizard();
		
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
		
		return null;
	}

}
