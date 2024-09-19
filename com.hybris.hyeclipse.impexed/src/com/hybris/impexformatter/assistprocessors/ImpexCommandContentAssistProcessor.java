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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.actions.Formatter;
import com.hybris.impexformatter.utils.StringHelper;

public class ImpexCommandContentAssistProcessor implements IContentAssistProcessor {

	private String exMsg = null;
	private IContextInformationValidator iciv;
	
	public ImpexCommandContentAssistProcessor() {
		super();
		iciv = new ContextInformationValidator(this);
	}
	
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer itv, int cursorPosition) {
		
		IDocument iDoc = itv.getDocument();
		IRegion lineInfo;
		try {
			lineInfo = iDoc.getLineInformationOfOffset(cursorPosition);
			String line = iDoc.get(lineInfo.getOffset(), lineInfo.getLength());
			List<String> keywords = Formatter.HEADER_MODE_PROPOSALS;
			List<ICompletionProposal> autoCompProposals = new ArrayList<>();
			//If the line is empty, propose Headers and remove leading whitespace...
			if (line.trim().length() == 0) {
				
				for (String keyword : keywords) {
					autoCompProposals.add(new CompletionProposal(keyword + " ", cursorPosition - line.length(), line.length(), keyword.length() + 1));
				}
				if (!autoCompProposals.isEmpty()) {
					return (ICompletionProposal[]) autoCompProposals.toArray(new ICompletionProposal[autoCompProposals.size()]);
				}
			}
			
			//Line isn't empty...
			for (String keyword : keywords) {
				if (StringHelper.findMatches(keyword, line)) {
					return standardMethod(itv, cursorPosition, keywords, autoCompProposals);
				}
			}
		}
		catch (BadLocationException ble) {
			Activator.getDefault().log("could not compute proposals", ble);
		}
		return null;
	}
	
	private ICompletionProposal[] standardMethod(ITextViewer viewer, int offset, List<String> keywords, List<ICompletionProposal> autoCompProposals) {
		
		int index = offset - 1;
		StringBuffer prefix = new StringBuffer();
		IDocument document = viewer.getDocument();
				
		while (index > 0) {
			try {
				char prev = document.getChar(index);
				if (Character.isWhitespace(prev)) {
					break;
				}
				prefix.insert(0, prev);
				index--;
			}
			catch (BadLocationException ble) {
				Activator.getDefault().log("could not get char", ble);
			}
		}

		if (prefix.length() > 0) {
			String word = prefix.toString();
			// limit to attributes
			if (containsOpenBracket(word)) {
				autoCompProposals.addAll(addHeaderAttributes(word, offset));
			}
			for (String keyword : keywords) {
				if (StringHelper.findMatches(keyword, word)) {
					autoCompProposals.add(new CompletionProposal(keyword + " ", index + 1, offset - (index + 1), keyword.length() + 1));
				}
			}
		}
		else {
			// propose header keywords
			proposeHeaderOperands(offset, document, autoCompProposals);
		}
		
		if (!autoCompProposals.isEmpty()) {
			return (ICompletionProposal[]) autoCompProposals.toArray(new ICompletionProposal[autoCompProposals.size()]);
		}
		return null;

	}

	private Collection<? extends ICompletionProposal> addHeaderAttributes(String oldWord, int offset) {
		
		List<String> keywords = Formatter.IMPEX_KEYWORDS_ATTRIBUTES;
		Collection<ICompletionProposal> result = Lists.newArrayList();
		int bracketPos = Optional.ofNullable(oldWord).orElse("").lastIndexOf("[");
		int wordLength = oldWord.length();
		int replacementPos = wordLength - bracketPos;
		if (bracketPos > -1) {
			String word = oldWord.substring(bracketPos);
			for (String keyword : keywords) {
				if (StringHelper.findMatches(keyword, word)) {					
					result.add(new CompletionProposal(keyword + "=", (offset - replacementPos) + 1, word.length(), keyword.length() + 1));
				}
			}	
		}
		
		return result;
		
	}

	/**
	 * Null-safe open-bracket character finder
	 * @param word - main string which is used for search
	 * @return return {@code true} when contains open bracket in text.
	 */
	private boolean containsOpenBracket(String word) {
		return Optional.ofNullable(word).orElse("").contains("[");
	}

	private void proposeHeaderOperands(int offset, IDocument document, List<ICompletionProposal> proposals) {
		
		String line = getCurrentLine(document, offset);
		if (!containsHeader(line)) {
			
			for (String keyword : Formatter.HEADER_MODE_PROPOSALS) {
				proposals.add(new CompletionProposal(keyword + " ", offset, 0, keyword.length() + 1));
			}
		}
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int cursorPosition) {

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
	
	public String getCurrentLine(IDocument document, int offset) {
		
		final String docContent = document.get();		
		String line = "";
		IRegion currentRegion = null;
		int posOffset = offset;
		try {
			currentRegion = document.getLineInformationOfOffset(offset);
			
			posOffset = currentRegion.getOffset() + currentRegion.getLength();
			
			line = docContent.substring(currentRegion.getOffset(), posOffset);
		}
		catch (BadLocationException ble) {
			Activator.getDefault().log("could not get current line", ble);
		}
		
		return line; 
	}
	
	public boolean containsHeader(String line) {
		String[] tokens = line.split(" ");
		ArrayList<String> tokenList = Lists.newArrayList(tokens);
		
		// uppercase tokens
		Collections2.transform(tokenList, new Function<String, String>() {

			@Override
			public String apply(String arg) {
				return arg.toUpperCase();
			}
		});
		
		Collection<String> result = Collections2.filter(tokenList, Predicates.in(Formatter.HEADER_MODE_PROPOSALS));
		return !result.isEmpty();
	}

}
