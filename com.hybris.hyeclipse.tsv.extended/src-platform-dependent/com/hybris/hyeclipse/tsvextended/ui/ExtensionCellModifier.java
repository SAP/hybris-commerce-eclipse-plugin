package com.hybris.hyeclipse.tsvextended.ui;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

import com.hybris.hyeclipse.tsvextended.utils.ExtensionHolder;

/**
 * This class implements an ICellModifier
 * An ICellModifier is called when the user modifies a cell in the 
 * tableViewer
 */
public class ExtensionCellModifier implements ICellModifier {
	
	private ModuleTableViewer moduleTableViewer;
	
	public ExtensionCellModifier(ModuleTableViewer moduleTableViewer) {
		super();
		this.moduleTableViewer = moduleTableViewer;
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (property.equalsIgnoreCase("name")) {
			return false;
		}
		
		return true;
	}

	@Override
	public Object getValue(Object element, String property) {
		// Find the index of the column
		int columnIndex = moduleTableViewer.getColumnNames().indexOf(property);
		Object result = null;
		ExtensionHolder extension = (ExtensionHolder)element;
		
		switch (columnIndex) {
			case 0 : // NAME_COLUMN 
				result = extension.getName();
				break;
			case 1 : // CORE_COLUMN 
				result = new Boolean(extension.isSelected());
				break;
			default :
				result = "";
		}
		return result;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		
		// Find the index of the column
		int columnIndex = moduleTableViewer.getColumnNames().indexOf(property);
		TableItem item = (TableItem) element;
		ExtensionHolder extension = (ExtensionHolder)item.getData();
			
		switch (columnIndex) {
			case 1 : // ANALYSE_COLUMN
				extension.setSelected(((Boolean) value).booleanValue());
				break;
			default :
		}
			
		//update list of extensions to be scanned
		moduleTableViewer.getTSVExtendedAnalyser().extensionChanged(extension);
	}

}
