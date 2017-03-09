package com.hybris.hyeclipse.tsvextended.ui;

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

import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsvextended.utils.ExtensionHolder;

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
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
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
		ExtensionHolder extension = (ExtensionHolder) element;
		switch (columnIndex) {
			case 1 :
				return getImage(extension.isSelected());
			default :
				return null; 	
		}
	
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String result = "";
		ExtensionHolder extension = (ExtensionHolder) element;
		switch (columnIndex) {
			case 0:  // NAME_COLUMN
				result = extension.getName();
				break;
			default :
				break; 	
		}
		return result;
	}
	
}
