package com.hybris.hyeclipse.emf.beans.presentation.platformdependent;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import de.hybris.bootstrap.typesystem.YType;

class YTypeLabelProvider extends BaseLabelProvider implements ILabelProvider {
	
	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return ((YType)element).getCode();
	}

}
