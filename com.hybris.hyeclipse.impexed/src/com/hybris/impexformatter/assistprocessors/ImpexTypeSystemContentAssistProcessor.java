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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.actions.Formatter;
import com.hybris.impexformatter.utils.StringHelper;

public class ImpexTypeSystemContentAssistProcessor implements IContentAssistProcessor {
	
	private Activator plugin;
	private IContextInformationValidator iciv;
	private String exMsg = null;
	
	public ImpexTypeSystemContentAssistProcessor() {
		super();
		iciv = new ContextInformationValidator(this);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer itv, int cursorPosition) {
		
		IDocument iDoc = itv.getDocument();
		
		try {
			IRegion lineInfo = iDoc.getLineInformationOfOffset(cursorPosition);
			
			int startOfLine = lineInfo.getOffset();
			int lineLength = lineInfo.getLength();
			int cursorPositionInLine = cursorPosition - startOfLine;
			
			String wholeLine = iDoc.get(startOfLine, lineLength);
			String lineAfterCursor = iDoc.get(startOfLine + cursorPositionInLine, lineLength - cursorPositionInLine);
			
			String lineWithoutCommand = "";
			lineWithoutCommand = wholeLine.substring(wholeLine.indexOf(" ") + 1, wholeLine.length());
			
			String lineWithoutCommandUpToCursor = lineWithoutCommand.substring(0, cursorPositionInLine - (wholeLine.indexOf(" ") + 1));
			
			//before cursor: lineWithoutCommandUpToCursor
			//after cursor: lineAfterCursor
			String tempString = new StringBuilder(lineWithoutCommandUpToCursor).append(";").append(lineAfterCursor).toString();
			String[] headerParts = tempString.split(";");
			
			boolean showTypes = false;
			boolean showAttributes = false;
			boolean showKeywords = false;
			
			int numberOfParts = headerParts.length;
			String currentPart = "";
			int lastSemicolon = lineWithoutCommandUpToCursor.lastIndexOf(";");
			currentPart = lineWithoutCommandUpToCursor.substring(lastSemicolon + 1, lineWithoutCommandUpToCursor.length());
			currentPart = currentPart.trim();
			
			//Do the easy checks first...
			if (lineWithoutCommand.trim().isEmpty()) {
				//If empty, show types
				showTypes = true;
			}
			else {
				if (currentPart.endsWith(";") || currentPart.endsWith("(")) {
					showAttributes = true;
				}
				else if (currentPart.endsWith("[")) {
					showKeywords = true;
				}
				else if (currentPart.endsWith("=")) {
					showKeywords = true;
				}
				else {
					//Now the more complicated checks...
					if (numberOfParts == 1 && !currentPart.isEmpty()) {
						showTypes = true;
					}
					else {
						if (currentPart.contains("[") && !currentPart.contains("]")) {
							showKeywords = true;
							currentPart = currentPart.substring(currentPart.lastIndexOf("[") + 1, currentPart.length());
						}
						else {
							showAttributes = true;
						}
					}
				}
			}
			
			if (showTypes || showAttributes) {
				if (plugin == null) {
					plugin = Activator.getDefault();
				}
			}
			
			List<String> autoSuggests = null;
			
			if (showTypes) {
				List<String> allTypeNames = plugin.getAllTypeNames();
				if (currentPart.isEmpty()) {
					//return all Types
					autoSuggests = new ArrayList<>(allTypeNames.size());
					autoSuggests.addAll(allTypeNames);
				}
				else {
					//If not empty, filter based on currentPart
					autoSuggests = new ArrayList<>();
					for (String typeCode : allTypeNames) {
						if (StringHelper.findMatches(typeCode, currentPart)) {
							autoSuggests.add(typeCode);
						}
					}
				}
			}
			
			if (showAttributes) {
				String typeName = headerParts[0];
				List<String> attributeNames = plugin.getAllAttributeNames(typeName);
				if (currentPart.isEmpty() || currentPart.endsWith(";") || currentPart.endsWith("(")) {
					//return all Attributes not already used
					autoSuggests = new ArrayList<>(attributeNames.size());
					for (String attributeQualifier : attributeNames) {
						if (!lineWithoutCommand.contains(attributeQualifier + ";") 
								&& !lineWithoutCommand.contains(";" + attributeQualifier)
								|| currentPart.endsWith("(")) {
							autoSuggests.add(attributeQualifier);
						}
					}
				}
				else {
					//If not empty, filter based on currentPart
					autoSuggests = new ArrayList<>();
					for (String attributeQualifier : attributeNames) {
						String tempCurrentPart = currentPart;
						boolean insideBrackets = false;
						if (currentPart.contains("(")) {
							tempCurrentPart = currentPart.substring(currentPart.indexOf("(") + 1, currentPart.length());
							insideBrackets = true;
						}
						//If its inside brackets, include all attributes
						//Otherwise, exclude those already used
						
						if (StringHelper.findMatches(attributeQualifier, tempCurrentPart)) {
							if (insideBrackets) {
								autoSuggests.add(attributeQualifier);
							}
							else {
								//Dont add already used attribute
								if (!lineWithoutCommand.contains(attributeQualifier + ";") 
								&& !lineWithoutCommand.contains(";" + attributeQualifier)
								|| currentPart.endsWith("(")) {
									autoSuggests.add(attributeQualifier);
								}
							}
						}
					}
				}
			}
			
			boolean includeEquals = true;
			boolean includesComma = false;
			if (showKeywords) {
				if (currentPart.isEmpty() || currentPart.endsWith("[") || currentPart.endsWith(",")) {
					if (currentPart.endsWith(",")) {
						includesComma = true;
						//already have 1, filter the list
						autoSuggests = new ArrayList<>();
						String keywordName = currentPart.substring(currentPart.indexOf("[") + 1, currentPart.lastIndexOf("="));
						for (String keyword : Formatter.IMPEX_KEYWORDS_ATTRIBUTES) {
							if (!keyword.equalsIgnoreCase(keywordName)) {
								autoSuggests.add(keyword);
							}
						}
					}
					else {
						autoSuggests = Formatter.IMPEX_KEYWORDS_ATTRIBUTES;
					}
				}
				else if (currentPart.trim().endsWith("=")) {
					if (currentPart.contains(",")) {
						includesComma = true;
					}
					//code[unique=
					String keywordName = "";
					if (includesComma) {
						keywordName = currentPart.substring(currentPart.lastIndexOf(",") + 1, currentPart.lastIndexOf("="));
					}
					else {
						keywordName = currentPart.substring(currentPart.indexOf("[") + 1, currentPart.indexOf("="));
					}
					if (keywordName.equalsIgnoreCase("mode")) {
						autoSuggests = Lists.newArrayList("append", "remove");
						includeEquals = false;
					}
					else {
						if (Formatter.IMPEX_KEYWORDS_ATTRIBUTES_BOOLEAN.contains(keywordName)) {
							autoSuggests = Lists.newArrayList("true", "false");
							includeEquals = false;
						}
					}
				}
				else {
					//If not empty, filter based on currentPart
					autoSuggests = new ArrayList<>();
					for (String keyword : Formatter.IMPEX_KEYWORDS_ATTRIBUTES) {
						if (StringHelper.findMatches(keyword, currentPart)) {
							autoSuggests.add(keyword);
						}
					}
				}
			}
			
			ICompletionProposal[] autoCompProposals = null;
			
			if (autoSuggests != null && autoSuggests.size() > 0) {
				Collections.sort(autoSuggests);
				
				if (showTypes)
					autoCompProposals = typeProposals(cursorPosition, autoSuggests, currentPart);
				if (showAttributes)
					autoCompProposals = attributeProposals(cursorPosition, autoSuggests, currentPart);
				if (showKeywords)
					autoCompProposals = keywordProposals(cursorPosition, autoSuggests, currentPart, includeEquals, includesComma);
			    exMsg = null;
			}
			
			return autoCompProposals;
		}
		catch (BadLocationException ble) {
			Activator.logError("BadLocationException", ble);
			exMsg = ble.getMessage();
			return null;
		}
	}
	
	private ICompletionProposal[] typeProposals(int cursorPosition, List<String> autoSuggests, String currentPart) {
		
		int counter = 0;
		ICompletionProposal[] proposals = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			//Each proposal contains the text to propose, as well as information about where to insert the text into the document. 
			proposals[counter] = new CompletionProposal(autoSuggest + ";", cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length() + 1);
			counter++;
		}
		return proposals;
	}
	
	private ICompletionProposal[] attributeProposals(int cursorPosition, List<String> autoSuggests, String currentPart) {
		
		int counter = 0;
		ICompletionProposal[] proposals = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			
			if (currentPart.endsWith(";") || currentPart.endsWith("(")) {
				proposals[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
			}
			else {
				int bracketStart = currentPart.indexOf("(");
				if (bracketStart > 0) {
					int a = (cursorPosition + 1) - (currentPart.length() - bracketStart);
					int b = currentPart.length() - (bracketStart + 1);
					int c = autoSuggest.length();
					proposals[counter] = new CompletionProposal(autoSuggest, a, b, c);
				}
				else {
					proposals[counter] = new CompletionProposal(autoSuggest, cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length());
				}
			}
			counter++;
		}
		return proposals;
	}

	private ICompletionProposal[] keywordProposals(int cursorPosition, List<String> autoSuggests, String currentPart, boolean includeEquals, boolean includesComma) {
		
		int counter = 0;
		ICompletionProposal[] proposals = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			
			if (currentPart.endsWith("[")) {
				proposals[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition, 0, autoSuggest.length() + 1);
			}
			else {
				if (includeEquals) {
					if (includesComma) {
						proposals[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition, 0, autoSuggest.length() + 1);
					}
					else {
						proposals[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length() + 1);
					}
				}
				else if (includesComma) {
					if (currentPart.endsWith("=")) {
						proposals[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
					}
					else {
						proposals[counter] = new CompletionProposal(autoSuggest, cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length() + 1);
					}
				}
				else {
					proposals[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
				}
			}
			counter++;
		}
		return proposals;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer textViewer, int cursorPosition) {
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
	public IContextInformationValidator getContextInformationValidator() {
		return iciv;
	}

	@Override
	public String getErrorMessage() {
		return exMsg;
	}

}
