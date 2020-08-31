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
package com.hybris.yps.hyeclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.hybris.yps.hyeclipse.utils.ProjectSourceUtil;

/**
 * Wizard to walk the user through attaching the sources to the projects in the workspace.
 * 
 * @author mheuer
 *
 */
public class AttachSourcesWizard extends Wizard
{
	private AttachSourcesPage	page;

	@Override
	public String getWindowTitle()
	{
		return "Attaching  Sources";
	}

	@Override
	public void addPages()
	{
		page = new AttachSourcesPage(false); // source archive is not optional
		addPage( page );
	}

	@Override
	public boolean canFinish() 
	{
		return page.isPageComplete();
	}
	
	@Override
	public boolean performFinish()
	{
		if (!page.validatePage()) 
		{
			MessageDialog
			.openError(
					getShell(),
					"Unreadable or non-existing file specified",
					"Please make sure the archive you selected is readable to the current user and exists." );
			// and ... abort
			return false;
		}
		
		File sourceArchive = page.getSourceFile();
		IRunnableWithProgress runner = ProjectSourceUtil.getRunner(sourceArchive);

		try
		{
			new ProgressMonitorDialog( getContainer().getShell() ).run( true, false, runner );

		}
		catch( InvocationTargetException | InterruptedException e )
		{
			Throwable t = (e instanceof InvocationTargetException) ? e.getCause() : e;
			MessageDialog.openError( getShell(), "Error attaching sources", t.toString() );
		}
		
		return true;
	}
}
