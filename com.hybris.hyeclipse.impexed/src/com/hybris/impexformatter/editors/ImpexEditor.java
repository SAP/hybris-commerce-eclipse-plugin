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
package com.hybris.impexformatter.editors;

//import org.eclipse.jface.text.source.AnnotationRulerColumn;
//import org.eclipse.jface.text.source.CompositeRuler;
//import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.ui.editors.text.TextEditor;

import com.hybris.impexformatter.DocumentProvider;

public class ImpexEditor extends TextEditor {

	public ImpexEditor() {
		super();
		setSourceViewerConfiguration(new ImpexSourceViewerConfig(getPreferenceStore()));
		setDocumentProvider(new DocumentProvider());
	}
	
	/*@Override
	protected IVerticalRuler createVerticalRuler() {
		int rulerWidth = 10;
		CompositeRuler ruler = new CompositeRuler();
		ruler.addDecorator(0, new AnnotationRulerColumn(rulerWidth));
		if (isLineNumberRulerVisible()) {
			ruler.addDecorator(1, createLineNumberRulerColumn());
		}
		return ruler;
	}*/
	
}
