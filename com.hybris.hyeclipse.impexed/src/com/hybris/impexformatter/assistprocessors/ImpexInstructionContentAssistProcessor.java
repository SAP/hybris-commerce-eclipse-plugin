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
package com.hybris.impexformatter.assistprocessors;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import com.google.common.collect.Lists;
import com.hybris.impexformatter.actions.Formatter;

public class ImpexInstructionContentAssistProcessor implements IContentAssistProcessor {

	private final static Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().toString());
	private IContextInformationValidator iciv;
	private String exMsg = null;
	
	public ImpexInstructionContentAssistProcessor() {
		super();
		iciv = new ContextInformationValidator(this);
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer itv, int cursorPosition) {
		
		int counter = cursorPosition - 1;
		StringBuffer prefix = new StringBuffer();
		IDocument iDoc = itv.getDocument();

		while (counter > 0) {
			try {
				char prev = iDoc.getChar(counter);
				if (Character.isWhitespace(prev)) {
					break;
				}
				prefix.insert(0, prev);
				counter--;
			}
			catch (BadLocationException ble) {
				LOG.log(Level.WARNING, "could not get char", ble);
			}
		}

		List<ICompletionProposal> autoCompProposals = new ArrayList<>();
		List<String> keywords = Lists.newArrayList(Formatter.INSTRUCTION_CLASS_PROPOSALS);

		if (prefix.length() > 0) {
			String word = prefix.toString();
			// limit to attributes
			for (String keyword : keywords) {
				if (keyword.toUpperCase(Locale.ENGLISH).startsWith(word.toUpperCase(Locale.ENGLISH)) && word.length() < keyword.length()) {
					autoCompProposals.add(new CompletionProposal(keyword + " ", counter + 1, cursorPosition - (counter + 1), keyword.length() + 1));
				}
			}
		}
		else {
			// Add all
			for (String keyword : keywords) {
				autoCompProposals.add(new CompletionProposal(keyword + " ", counter + 1, cursorPosition - (counter + 1), keyword.length() + 1));
			}
		}
		if (!autoCompProposals.isEmpty()) {
			return (ICompletionProposal[]) autoCompProposals.toArray(new ICompletionProposal[autoCompProposals.size()]);
		}
		
		return new ICompletionProposal[0] ;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer itv, int cursorPosition) {
		exMsg = "No Context Information available";
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {

		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {

		return null;
	}

	@Override
	public String getErrorMessage() {
		return exMsg;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return iciv;
	}

	public String getCurrentLine(IDocument document, int cursorPosition, boolean toCursor) {
		final String docContent = document.get();
		String line = "";
		IRegion currentRegion = null;
		int posOffset = cursorPosition;
		try {
			currentRegion = document.getLineInformationOfOffset(cursorPosition);
			if (toCursor == false) {
				posOffset = currentRegion.getOffset() + currentRegion.getLength();
			}
			line = docContent.substring(currentRegion.getOffset(), posOffset);
		}
		catch (BadLocationException ble) {
			LOG.log(Level.WARNING, "could not get current line", ble);
		}
		return line;
	}

}
