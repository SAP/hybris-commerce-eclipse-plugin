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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsv.model.TSVResult;
import com.hybris.hyeclipse.tsv.preferences.PreferenceConstants;

public class TSVResultsLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

	FontRegistry registry = new FontRegistry();
	IPreferenceStore store = Activator.getDefault().getPreferenceStore();
	Display display = Display.getCurrent();
	Color highColour = new Color(display, PreferenceConverter.getColor(store, PreferenceConstants.P_PRIORITY_HIGH_COLOR));
	Color mediumColour = new Color(display, PreferenceConverter.getColor(store, PreferenceConstants.P_PRIORITY_MEDIUM_COLOR));
	Color lowColour = new Color(display, PreferenceConverter.getColor(store, PreferenceConstants.P_PRIORITY_LOW_COLOR));
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof String) {
			if (columnIndex == 0) {
				return (String) element;
			}
		}
		else if (element instanceof TSVResult) {
			TSVResult result = (TSVResult)element;
			switch(columnIndex) {
			case 0:
				return result.getElement();
			case 1:
				return new String(Integer.toString(result.getLineNumber()));
			case 2:
				return result.getRuleId();
			case 3:
				return result.getDescription();
			}
		}
		return "";
	}

	@Override
	public Color getBackground(Object element, int columnIndex) {
		
		return null;
	}

	@Override
	public Color getForeground(Object element, int columnIndex) {
		if (element instanceof TSVResult) {
			TSVResult result = (TSVResult)element;
			
			if (result.getRulePriority().equalsIgnoreCase("H")) {
				
				return highColour;
			}
			else if (result.getRulePriority().equalsIgnoreCase("M")) {
				
				return mediumColour;
			}
			else if (result.getRulePriority().equalsIgnoreCase("L")) {
				
				return lowColour;
			}
		}
		return null;
	}


	@Override
	public Font getFont(Object element, int columnIndex) {
		if (element instanceof String) {
			return registry.getBold(Display.getCurrent().getSystemFont()
				.getFontData()[0].getName());
		}
		return null;
	}

}
