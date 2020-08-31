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

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

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
		
		String typeLocation = Activator.getDefault().getTypeLoaderInfo(location);
		String fileName = typeLocation.substring(0, typeLocation.indexOf(":"));
		String extensionName = fileName.replaceAll("-items.xml", "");
		String lineNumberStr = typeLocation.substring(typeLocation.indexOf(":") + 1, typeLocation.indexOf("("));
		
		IProject extension = ResourcesPlugin.getWorkspace().getRoot().getProject(extensionName);
    	IFile itemsxml = extension.getFile("resources/" + fileName);
    	if (itemsxml.exists()) {
        	IMarker marker;
        	try {
				marker = itemsxml.createMarker(IMarker.TEXT);
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put(IMarker.LINE_NUMBER, Integer.parseInt(lineNumberStr));
				marker.setAttributes(map);
				//IDE.openEditor(getSite().getPage(), marker);
				IDE.openEditor(page, marker);
				marker.delete();
			}
        	catch (CoreException e) {
        		Activator.logError("Eclipse CoreException", e);
			}
    	}
    	else {
    		MessageBox dialog = new MessageBox(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.ICON_WARNING | SWT.OK);
    		dialog.setText("Extension not found");
    		dialog.setMessage("The extension " + extensionName + " was not found in the workspace. Please import it and try again.");
    		dialog.open();
    	}
	}

}
