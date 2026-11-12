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
package com.hybris.impexformatter.hyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.hybris.hyeclipse.commons.typesystem.YType;
import com.hybris.impexformatter.Activator;

public class ImpexTypeHyperlink implements IHyperlink {
	
	private final IRegion fUrlRegion;
	private String location;

	public ImpexTypeHyperlink(IRegion targetRegion, String text) {
		fUrlRegion = targetRegion;
		location = text;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return fUrlRegion;
	}

	@Override
	public String getHyperlinkText() {
		
		return null;
	}

	@Override
	public String getTypeLabel() {
		
		return null;
	}

	@Override
	public void open() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		
		final YType type = Activator.getDefault().getTypeSystemSnapshot().getType(location);
		if (type == null || page == null) {
			return;
		}
		final IFile source = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(type.getSourcePath()));
		if (!source.exists()) {
			return;
		}
		try {
			final IMarker marker = source.createMarker(IMarker.TEXT);
			try {
				marker.setAttribute(IMarker.LINE_NUMBER, type.getSourceLine());
				IDE.openEditor(page, marker);
			} finally {
				marker.delete();
			}
		} catch (CoreException e) {
			Activator.logError("Could not open generated model source", e);
		}
	}

}
