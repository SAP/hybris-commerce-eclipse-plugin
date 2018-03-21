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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.hybris.hyeclipse.impex.importer.dialogs.ImpexImportWithMacroDialog;
import com.hybris.hyeclipse.impex.importer.managers.ImportManager;

/**
 * Handler class for impex importing with a separate macro file.
 */
public class ImpexImportWithMacroHandler extends AbstractHandler {
	private static final String IMPORT_DIALOG_TITLE = "Impex Import with Macro";
	private static final String ENCODING = "UTF-8";
	
	private final ImportManager importManager = new ImportManager();

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
		
		ImpexImportWithMacroDialog dialog = new ImpexImportWithMacroDialog(workbenchWindow.getShell());
		int result = dialog.open();
		
		if (result == Window.OK) {
			final StringBuilder impexContent = new StringBuilder();
			appendFileToContent(dialog.getMacroFileName(), impexContent);
			appendFileToContent(dialog.getImpexFileName(), impexContent);

			final Shell shell = HandlerUtil.getActiveShell(event);
			final String message = importManager.performImport(impexContent.toString());
			MessageDialog.openInformation(shell, IMPORT_DIALOG_TITLE, message);
		}		
		
		return null;
	}

	
	private void appendFileToContent(String fileName, StringBuilder content) {
		final File file = FileUtils.getFile(fileName);
		try {
			content.append(FileUtils.readFileToString(file, ENCODING));
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
	
}
