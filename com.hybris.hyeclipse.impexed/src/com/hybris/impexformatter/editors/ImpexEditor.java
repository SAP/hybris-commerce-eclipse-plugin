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
