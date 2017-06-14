package com.hybris.hyeclipse.testers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract property tester class, with method to test the file extension
 */
public abstract class AbstractFilePropertyTester extends PropertyTester {

	/**
	 * Check whether selected file matches set of extension
	 * 
	 * @param receiver selected file
	 * @param fileExtension extension to compare
	 * @return true if selected file matches at extension, false otherwise.
	 */
	protected boolean testSelectedFileByExtension(final Object receiver, final String fileExtension) {
		return testSelectedFileByExtensions(receiver, new HashSet<>( Arrays.asList(fileExtension) ));
	}
	
	/**
	 * Check whether selected file matches set of extensions 
	 * 
	 * @param receiver selected file
	 * @param fileExtensions set of extensions to compare
	 * @return true if selected file matches at least one extensions, false otherwise.
	 */
	@SuppressWarnings({"unchecked", "restriction"})
	protected boolean testSelectedFileByExtensions(final Object receiver, final Set<String> fileExtensions) {
		if (receiver instanceof Set) {
			final Set<Object> set = (Set<Object>) receiver;
			
			if (set.size() == 1 && set.iterator().next() instanceof TextSelection) {
				final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				final IFile file = (IFile) window.getActivePage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
				
				return fileExtensions.contains(file.getFileExtension());
			}
		} else if (receiver instanceof List) {
			boolean doesExtensionMatch = false;
			final List<Object> list = (List<Object>) receiver;
			for( Object directoryContent : list ) {
				if( directoryContent instanceof File  ) {
					final File file = (File) directoryContent;
					doesExtensionMatch = true;
					
					if( !fileExtensions.contains(file.getFileExtension()) ) {
						return false;
					}
				}
			}

			return doesExtensionMatch;
		}
		return false;
	}
}
