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
package com.hybris.impexformatter.hyperlink;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.hybris.impexformatter.Activator;

public class ImpexTypeHyperlinkDetector extends AbstractHyperlinkDetector implements IHyperlinkDetector {
	
	private List<String> allTypeNames;

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		
		IDocument document = textViewer.getDocument();
		int offset = region.getOffset();

		// extract relevant characters
		IRegion lineRegion;
		String candidate;
		try {
			lineRegion = document.getLineInformationOfOffset(offset);
			candidate = document.get(lineRegion.getOffset(), lineRegion.getLength());
		}
		catch (BadLocationException ex) {
			return null;
		}
		
		String upperCandidate = candidate.toUpperCase(Locale.ENGLISH);
		if (upperCandidate.startsWith("INSERT") || upperCandidate.startsWith("REMOVE") || upperCandidate.startsWith("UPDATE")) {
			// look for keyword
			if (allTypeNames == null) {
//				allTypeNames = Activator.getDefault().getAllTypeNames();
//				TODO get all type names				
			}
		
			String headerPlusType = candidate.substring(0, candidate.indexOf(";"));
			String typeName = headerPlusType.substring(candidate.indexOf(" ") + 1, headerPlusType.length());
			if (allTypeNames.contains(typeName)) {
		
				int index = candidate.indexOf(typeName);
				if (index != -1) {
	
					// detect region containing keyword
					IRegion targetRegion = new Region(lineRegion.getOffset() + index, typeName.length());
					if ((targetRegion.getOffset() <= offset) && ((targetRegion.getOffset() + targetRegion.getLength()) > offset)) {
						try {
							return new IHyperlink[] { new ImpexTypeHyperlink(targetRegion, document.get(targetRegion.getOffset(), targetRegion.getLength())) };
						}
						catch (BadLocationException e) {
							Activator.logError("BadLocationException", e);
						}
					}
				}
			}
		}

		return null;
	}

}
