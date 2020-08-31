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
package com.hybris.hyeclipse.commons.testers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.internal.resources.File;
import org.eclipse.jface.text.TextSelection;

import com.hybris.hyeclipse.commons.utils.EclipseFileUtils;

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
		return testSelectedFilesByExtensions(receiver, new HashSet<>( Arrays.asList(fileExtension) ));
	}
	
	/**
	 * Check whether selected file(s) matches set of extensions 
	 * 
	 * @param receiver selected file(s)
	 * @param fileExtensions set of extensions to compare
	 * @return true if selected file(s) matches at least one extensions, false otherwise.
	 */
	@SuppressWarnings({"unchecked", "restriction"})
	protected boolean testSelectedFilesByExtensions(final Object receiver, final Set<String> fileExtensions) {
		boolean isValid = false;
		
		if (receiver instanceof Set) {
			final Set<Object> activeEditorSet = (Set<Object>) receiver;

			if (activeEditorSet.size() == 1 && activeEditorSet.iterator().next() instanceof TextSelection) {
				isValid = fileExtensions.contains(EclipseFileUtils.getActiveEditorFile().getFileExtension());
			}
		} else if (receiver instanceof List) {
			final List<Object> fileList = (List<Object>) receiver;
			
			if( !fileList.isEmpty() ) {
				isValid = fileList.stream()
								.filter(file -> file instanceof File)
								.map(File.class::cast)
								.map(File::getFileExtension)
								.allMatch(fileExtensions::contains);
			}
			
		}
		return isValid;
	}
}
