package com.hybris.hyeclipse.tsv.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.hybris.hyeclipse.tsv.Activator;

public class TSVPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public TSVPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Colour");
	}
	
	public void createFieldEditors() {
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_HIGH_COLOR, "High Priority: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_MEDIUM_COLOR, "Medium Priority: ", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_PRIORITY_LOW_COLOR, "Low Priority: ", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}
	
}