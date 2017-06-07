package com.hybris.hyeclipse.editor.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.editor.Activator;
import com.hybris.hyeclipse.editor.ui.MultiLineStringFieldEditor;

public class EditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final int FIELD_WIDTH = 5;

	private static interface LABELS {
		static final String COPYRIGHT_CONTENTS = "Copyright contents: ";
		static final String COPYRIGHT_FIRST_LINE = "First line: ";
		static final String COPYRIGHT_LINE_PREFIX = "Line prefix: ";
		static final String COPYRIGHT_LAST_LINE = "Last line: ";
	}

	private static final String COPYRIGHT_PREFERENCES = "Copyright preferences";
	private StringFieldEditor copyrightContent;
	private StringFieldEditor firstLine;
	private StringFieldEditor linePrefix;
	private StringFieldEditor lastLine;

	public EditorPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(final IWorkbench arg0) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(COPYRIGHT_PREFERENCES);
	}

	@Override
	protected void createFieldEditors() {
		copyrightContent = new MultiLineStringFieldEditor(EditorPreferenceConstants.COPYRIGHT_CONTENT,
				LABELS.COPYRIGHT_CONTENTS, getFieldEditorParent());
		firstLine = new StringFieldEditor(EditorPreferenceConstants.COPYRIGHT_FIRST_LINE, LABELS.COPYRIGHT_FIRST_LINE,
				FIELD_WIDTH, getFieldEditorParent());
		linePrefix = new StringFieldEditor(EditorPreferenceConstants.COPYRIGHT_LINE_PREFIX,
				LABELS.COPYRIGHT_LINE_PREFIX, FIELD_WIDTH, getFieldEditorParent());
		lastLine = new StringFieldEditor(EditorPreferenceConstants.COPYRIGHT_LAST_LINE, LABELS.COPYRIGHT_LAST_LINE,
				FIELD_WIDTH, getFieldEditorParent());
		addField(copyrightContent);
		addField(firstLine);
		addField(linePrefix);
		addField(lastLine);
	}

}
