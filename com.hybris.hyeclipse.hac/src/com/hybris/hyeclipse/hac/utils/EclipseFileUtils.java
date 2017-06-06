package com.hybris.hyeclipse.hac.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class to work with files in eclipse.
 */
public final class EclipseFileUtils {

	/**
	 * Private in order to avoid class initialization
	 */
	private EclipseFileUtils() { /* Intentionally empty */ }
	
	/**
	 * Returns selected file
	 * 
	 * @param selection
	 *            current selection
	 * @return selected {@link IFile}
	 */
	public static IFile getSelectedFile(final ISelection selection) {
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
