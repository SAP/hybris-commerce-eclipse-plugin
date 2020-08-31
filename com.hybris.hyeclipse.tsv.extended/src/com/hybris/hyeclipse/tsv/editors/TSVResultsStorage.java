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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

public class TSVResultsStorage extends PlatformObject implements IStorage {
	
	private String resultsString;
	private IPath path;
	
	public TSVResultsStorage(String resultsString, IPath path) {
		this.resultsString = resultsString;
		this.path = path;
	}

	@Override
	public InputStream getContents() throws CoreException {
		
		return new ByteArrayInputStream(resultsString.getBytes());
	}
	
	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public IPath getFullPath() {
		return path;
	}

	@Override
	public String getName() {
		return "analysis.tsv";
	}
	
}
