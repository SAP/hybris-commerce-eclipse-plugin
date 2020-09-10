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
package com.hybris.hyeclipse.editor.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hybris.hyeclipse.editor.Activator;
import com.hybris.hyeclipse.editor.ui.MultiLineStringFieldEditor;

/**
 * Copyright preference page
 */
public class CopyrightPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final int FIELD_WIDTH = 5;

	private final class LABELS {
		static final String COPYRIGHT_CONTENTS = "Copyright contents: ";
		static final String COPYRIGHT_FIRST_LINE = "First line: ";
		static final String COPYRIGHT_LINE_PREFIX = "Line prefix: ";
		static final String COPYRIGHT_LAST_LINE = "Last line: ";
		static final String COPYRIGHT_PREFERENCES = "Copyright preferences";
	}

	public CopyrightPreferencePage() {
		super(GRID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench arg0) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(LABELS.COPYRIGHT_PREFERENCES);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {
		StringFieldEditor copyrightContent = new MultiLineStringFieldEditor(CopyrightPreferenceConstants.COPYRIGHT_CONTENT,
				LABELS.COPYRIGHT_CONTENTS, getFieldEditorParent());
		StringFieldEditor firstLine = new StringFieldEditor(CopyrightPreferenceConstants.COPYRIGHT_FIRST_LINE,
				LABELS.COPYRIGHT_FIRST_LINE, FIELD_WIDTH, getFieldEditorParent());
		StringFieldEditor linePrefix = new StringFieldEditor(CopyrightPreferenceConstants.COPYRIGHT_LINE_PREFIX,
				LABELS.COPYRIGHT_LINE_PREFIX, FIELD_WIDTH, getFieldEditorParent());
		StringFieldEditor lastLine = new StringFieldEditor(CopyrightPreferenceConstants.COPYRIGHT_LAST_LINE, LABELS.COPYRIGHT_LAST_LINE,
				FIELD_WIDTH, getFieldEditorParent());
		addField(copyrightContent);
		addField(firstLine);
		addField(linePrefix);
		addField(lastLine);
	}

}
