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
package com.hybris.hyeclipse.tsv.validator;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import com.hybris.hyeclipse.commons.validation.ValidationResult;
import com.hybris.hyeclipse.commons.validation.ValidationService;

public class ItemsXmlValidator {
	private final ValidationService service = new ValidationService();

	/** @param itemsXmlLocation input file @param monitor cancellation @return validation findings */
	public List<ValidationResult> analyze(final File itemsXmlLocation, final IProgressMonitor monitor) {
		return service.analyze(itemsXmlLocation, monitor);
	}
	
	public List<ValidationResult> analyze(final IFile itemsXmlFile, final IProgressMonitor monitor) {
		return analyze(itemsXmlFile.getLocation().toFile(), monitor);
	}

}
