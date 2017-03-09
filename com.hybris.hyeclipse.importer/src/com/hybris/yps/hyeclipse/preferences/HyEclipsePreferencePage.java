package com.hybris.yps.hyeclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class HyEclipsePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public HyEclipsePreferencePage() {
		super(GRID);
		setDescription("SAP Hybris Commerce, development tools for Eclipse is a suite of Eclipse plugins for SAP Hybris Commerce projects that improves developer productivity.\n\nSee https://wiki.hybris.com/display/hyps/SAP+Hybris+Commerce+development+tools+for+Eclipse for details.");
	}
	
	@Override
	public void init(IWorkbench arg0) {}

	@Override
	protected void createFieldEditors() {}

}
