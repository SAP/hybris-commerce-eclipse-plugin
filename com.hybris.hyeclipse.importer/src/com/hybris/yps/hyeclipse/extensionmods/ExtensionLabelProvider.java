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
package com.hybris.yps.hyeclipse.extensionmods;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

//import com.hybris.yps.hyeclipse.ExtensionHolder;

/**
 * Label provider for the ModuleTableViewer
 * 
 * @see org.eclipse.jface.viewers.LabelProvider 
 */
public class ExtensionLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	// Names of images used to represent checkboxes
	public static final String CHECKED_IMAGE 	= "checked";
	public static final String UNCHECKED_IMAGE  = "unchecked";
	
	// For the checkbox images
	private static ImageRegistry imageRegistry = new ImageRegistry();
	
	/**
	 * Note: An image registry owns all of the image objects registered with it,
	 * and automatically disposes of them when the SWT Display is disposed.
	 */ 
	static {
		String iconPath = "icon/";
		Bundle bundle = FrameworkUtil.getBundle(ExtensionLabelProvider.class);
		URL url = FileLocator.find(bundle, new Path(iconPath + CHECKED_IMAGE + ".gif"), null);
		imageRegistry.put(CHECKED_IMAGE, ImageDescriptor.createFromURL(url));
		url = FileLocator.find(bundle, new Path(iconPath + UNCHECKED_IMAGE + ".gif"), null);
		imageRegistry.put(UNCHECKED_IMAGE, ImageDescriptor.createFromURL(url));
	}
	
	/**
	 * Returns the image with the given key, or <code>null</code> if not found.
	 */
	private Image getImage(boolean isSelected) {
		String key = isSelected ? CHECKED_IMAGE : UNCHECKED_IMAGE;
		return  imageRegistry.get(key);
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public Image getColumnImage(Object element, int columnIndex) {
//		ExtensionHolder extension = (ExtensionHolder) element;
//		switch (columnIndex) {
//			case 0:  // NAME_COLUMN
//				return null;
//			case 1 :
//				return getImage(extension.isCoreModule());
//			case 2 :
//				return getImage(extension.isWebModule());
//			case 3 :
//				return getImage(extension.isHmcModule());
//			default :
//				return null; 	
//		}
//	
//	}

//	@Override
//	public String getColumnText(Object element, int columnIndex) {
//		String result = "";
//		ExtensionHolder extension = (ExtensionHolder) element;
//		switch (columnIndex) {
//			case 0:  // NAME_COLUMN
//				result = extension.getName();
//				break;
//			default :
//				break; 	
//		}
//		return result;
//	}
	
}
