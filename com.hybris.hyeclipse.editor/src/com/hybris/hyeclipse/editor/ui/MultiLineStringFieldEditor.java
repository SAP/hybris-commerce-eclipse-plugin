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
package com.hybris.hyeclipse.editor.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * String editor allowing display of multiple lines
 */
public class MultiLineStringFieldEditor extends StringFieldEditor {
	private static final String UNKNOWN_VALIDATE_STRATEGY = "Unknown validate strategy";
	private Text textFieldML = null;
	private int validateStrategyML = VALIDATE_ON_KEY_STROKE;
	private int textLimitML = UNLIMITED;

	public MultiLineStringFieldEditor(final String name, final String labelText, final int width, final int strategy,
			final Composite parent) {
		super(name, labelText, width, strategy, parent);
		setValidateStrategy(strategy);
	}

	public MultiLineStringFieldEditor(final String name, final String labelText, final int width,
			final Composite parent) {
		this(name, labelText, width, VALIDATE_ON_KEY_STROKE, parent);
	}

	public MultiLineStringFieldEditor(final String name, final String labelText, final Composite parent) {
		this(name, labelText, UNLIMITED, parent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setValidateStrategy(final int value) {
		super.setValidateStrategy(value);
		Assert.isTrue(value == VALIDATE_ON_FOCUS_LOST || value == VALIDATE_ON_KEY_STROKE);
		validateStrategyML = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTextLimit(final int limit) {
		super.setTextLimit(limit);
		textLimitML = limit;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Text getTextControl(final Composite parent) {
		textFieldML = super.getTextControl();
		if (textFieldML == null) {
			textFieldML = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.LEFT | SWT.V_SCROLL);
			textFieldML.setFont(parent.getFont());
			switch (validateStrategyML) {
			case VALIDATE_ON_KEY_STROKE:
				textFieldML.addKeyListener(new KeyAdapter() {

					@Override
					public void keyReleased(final KeyEvent e) {
						valueChanged();
					}
				});

				break;
			case VALIDATE_ON_FOCUS_LOST:
				textFieldML.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(final KeyEvent e) {
						clearErrorMessage();
					}
				});
				textFieldML.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(final FocusEvent e) {
						refreshValidState();
					}

					@Override
					public void focusLost(final FocusEvent e) {
						valueChanged();
						clearErrorMessage();
					}
				});
				break;
			default:
				Assert.isTrue(false, UNKNOWN_VALIDATE_STRATEGY);
			}
			textFieldML.addDisposeListener(e -> textFieldML = null);
			if (textLimitML > 0) {
				textFieldML.setTextLimit(textLimitML);
			}
		} else {
			checkParent(textFieldML, parent);
		}
		return textFieldML;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doFillIntoGrid(final Composite parent, final int numColumns) {
		super.doFillIntoGrid(parent, numColumns);

		textFieldML = super.getTextControl();
		GridData gd = (GridData) textFieldML.getLayoutData();
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		textFieldML.setLayoutData(gd);

		final Label label = getLabelControl(parent);
		gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		label.setLayoutData(gd);
	}
}
