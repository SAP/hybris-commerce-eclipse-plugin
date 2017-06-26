package com.hybris.hyeclipse.commons.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility class to work with files in eclipse.
 */
public final class EclipseFileUtils {

	/**
	 * Private in order to avoid class initialization
	 */
	private EclipseFileUtils() {
		/* Intentionally empty */ }

	/**
	 * Returns selected file
	 * 
	 * @param selection
	 *            current selection
	 * @return selected {@link IFile}
	 */
	public static IFile getSelectedFile(final ISelection selection) {
		IFile file = null;
		
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			final Object obj = ssel.getFirstElement();
			file = (IFile) Platform.getAdapterManager().getAdapter(obj, IFile.class);
		} else if (selection instanceof TextSelection) {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			file = (IFile) window.getActivePage().getActiveEditor().getEditorInput()
			                .getAdapter(IFile.class);
		}
		return file;
	}

	/**
	 * Return set of selected file(s)
	 * 
	 * @param selection
	 *            current selection
	 * @return set of selected files
	 */
	@SuppressWarnings("unchecked")
	public static Set<IFile> getSelectedFiles(final ISelection selection) {
		final Set<IFile> files = new HashSet<>();

		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			structuredSelection.toList().forEach(
			                file -> files.add((IFile) Platform.getAdapterManager().getAdapter(file, IFile.class)));
		} else if (selection instanceof TextSelection) {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			files.add((IFile) window.getActivePage().getActiveEditor().getEditorInput().getAdapter(IFile.class));

		}

		return files;
	}

	/**
	 * Returns optional of current text selection
	 *  
	 * @return optional of current text selection
	 */
	public static Optional<TextSelection> getCurrentTextSelection() {
		Optional<TextSelection> currentTextSelection = Optional.empty();
		final IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
		                .getActiveEditor();
		
		if ( editorPart != null && editorPart instanceof ITextEditor) {
			final ITextEditor editor = (ITextEditor) editorPart;
			final ISelection selection = editor.getSelectionProvider().getSelection();
			if (selection instanceof TextSelection) {
				currentTextSelection = Optional.of((TextSelection) selection);
			}
		}
		
		return currentTextSelection;
	}

	/**
	 * Returns selected text in file
	 * 
	 * @return selected text in file, if none is selected empty string will be returned
	 */
	public static String getCurrentSelectedText() {
		final Optional<TextSelection> currentTextSelection = getCurrentTextSelection();
		return (currentTextSelection.isPresent()) ? currentTextSelection.get().getText() : Constants.EMPTY_STRING;
	}
	
	/**
	 * Return content of multiple files as a String. Adds new line to the end of each file.
	 * 
	 * @param Set
	 *            of files
	 * @return content of multiple files as a String
	 */
	public static String getContentOfFiles(final Set<IFile> files) {
		final StringBuilder filesContent = new StringBuilder();

		files.forEach(file -> filesContent.append(getContentOfFile(file)).append(Constants.NEW_LINE));

		return filesContent.toString();
	}

	/**
	 * Returns the content of file as {@link String}
	 *
	 * @param file
	 *            file from which content is retrieved
	 * @return content of a file
	 */
	public static String getContentOfFile(final IFile file) {
		try {
			return IOUtils.toString(file.getContents(), Constants.UTF_8_ENCODING);
		} catch (CoreException | IOException e) {
			ConsoleUtils.printError(e.getMessage());
		}
		return Constants.EMPTY_STRING;
	}
}
