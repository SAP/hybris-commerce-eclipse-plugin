package com.hybris.hyeclipse.impex.importer.handlers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.hac.utils.ConsoleUtils;
import com.hybris.hyeclipse.impex.importer.managers.ImportManager;

/**
 * Handler class for impex importing
 */
public class ImpexImportHandler extends AbstractHandler {
	private final String IMPORT_DIALOG_TITLE = "Impex Import";
	private static final String ENCODING = "UTF-8";
	private final ImportManager importManager = new ImportManager();

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IFile impexFile = getSelectedFile(HandlerUtil.getCurrentSelection(event));
		final Shell shell = HandlerUtil.getActiveShell(event);
		
		final String message = importManager.performImport(getContentOfFile(impexFile));
		MessageDialog.openInformation(shell, IMPORT_DIALOG_TITLE, message);
		return null;
	}

	/**
	 * Returns selected file
	 * 
	 * @param selection
	 *            current selection
	 * @return selected {@link IFile}
	 */
	private IFile getSelectedFile(final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			final Object obj = ssel.getFirstElement();
			final IFile file = (IFile) Platform.getAdapterManager().getAdapter(obj, IFile.class);
			return file;
		} else if (selection instanceof TextSelection) {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			final IFile file = (IFile) window.getActivePage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
			return file;
		}
		return null;
	}

	
	/**
	 * Returns the content of file as {@link String}
	 *
	 * @param file
	 *            file from which content is retrieved
	 * @return content of a file
	 */
	protected String getContentOfFile(final IFile file) {
		try {
			return IOUtils.toString(file.getContents(), ENCODING);
		} catch (CoreException | IOException exception) {
			ConsoleUtils.printError(exception.getMessage());
		}
		return "";
	}
	
}
