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
package com.hybris.hyeclipse.tsv.editors;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class TSVResultsInput extends PlatformObject implements IStorageEditorInput {
	
	private IStorage storage;
	private String tooltipText = "TSV Extended Analysis";

	public TSVResultsInput(IStorage storage) {
		this.storage = storage;
	}
	
	public TSVResultsInput(IStorage storage, String tooltipText) {
		this.storage = storage;
		this.tooltipText = tooltipText;
	}

	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public IStorage getStorage() throws CoreException {
		return storage;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "Raw";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return tooltipText;
	}

}
