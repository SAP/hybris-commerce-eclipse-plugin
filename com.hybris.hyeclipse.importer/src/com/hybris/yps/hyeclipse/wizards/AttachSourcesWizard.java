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
		return "Attaching [y] Sources";
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
