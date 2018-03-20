package com.hybris.hyeclipse.impex.importer.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
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

	private final String IMPORTER_DIALOG_TITLE = "Importer";
	private final ImportManager importManager = new ImportManager();

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		System.out.println("ImpexImportWithMacroHandler.execute()");
		
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
		ImpexImportWithMacroDialog impexImportWithMacroDialog = new ImpexImportWithMacroDialog(workbenchWindow.getShell());
		int result = impexImportWithMacroDialog.open();
		
		if (result == Window.OK) {
			System.out.println("ImpexImportWithMacroHandler.execute() - OK");
			final String impexFileName = impexImportWithMacroDialog.getImpexFileName();
			final String macroFileName = impexImportWithMacroDialog.getMacroFileName();
			
			System.out.println("impexFileName=[" + impexFileName + "]");
			System.out.println("macroFileName=[" + macroFileName + "]");
		} else {
			System.out.println("ImpexImportWithMacroHandler.execute() - CANCEL");			
		}
		
		
		return null;
		
		// 
		
		
		/* from ImpexImportHandler
		final IFile impexFile = getSelectedFile(HandlerUtil.getCurrentSelection(event));
		final Shell shell = HandlerUtil.getActiveShell(event);
		final String message = importManager.performImport(impexFile);
		MessageDialog.openInformation(shell, IMPORTER_DIALOG_TITLE, message);
		return null;
		*/
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

}
