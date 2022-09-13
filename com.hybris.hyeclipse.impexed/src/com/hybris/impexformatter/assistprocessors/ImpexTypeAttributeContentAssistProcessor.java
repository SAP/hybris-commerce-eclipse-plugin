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

import static com.hybris.impexformatter.utils.StringHelper.findMatches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

import com.google.common.collect.Lists;
import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.actions.Formatter;

public class ImpexTypeAttributeContentAssistProcessor implements IContentAssistProcessor {
	
	private Activator plugin;
	private IContextInformationValidator iciv;
	private String exMsg = null;
	
	public ImpexTypeAttributeContentAssistProcessor() {
		super();
		iciv = new ContextInformationValidator(this);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer itv, int cursorPosition) {
		
		//TODO
		//Add variables defined at top of page to attribute list????
		
		IDocument document = itv.getDocument(); 
		
		try {
			String thisAttrib = "";
			
			IRegion lineInfo = document.getLineInformationOfOffset(cursorPosition);
			String line = document.get(lineInfo.getOffset(), lineInfo.getLength());
			if (line.endsWith("]") || line.endsWith(")")) {
				line = line.substring(0, line.length() -1);
			}
			
			thisAttrib = line.substring(line.indexOf(" ") + 1, line.length());
			
			String[] headerParts = Optional.ofNullable(thisAttrib).orElse("").split(";");
			
			
			
			
			boolean showTypes = false;
			boolean showAttributes = false;
			boolean showKeywords = false;
			
			int numberOfParts = headerParts.length;
			String currentPart = "";
			
			//Do the easy checks first...
			if (thisAttrib.trim().isEmpty()) {
				//If empty, show types
				showTypes = true;
			}
			else {
				currentPart = headerParts[numberOfParts -1];
				currentPart = currentPart.trim();
				
				if (thisAttrib.endsWith(";") || thisAttrib.endsWith("(")) {
					showAttributes = true;
				}
				else if (thisAttrib.endsWith("[")) {
					showKeywords = true;
				}
				else if (thisAttrib.endsWith("=")) {
					showKeywords = true;
				}
				else {
					//Now the more complicated checks...
					if (numberOfParts == 1) {
						showTypes = true;
					}
					else {
						//.........
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
				if (thisAttrib.trim().isEmpty()) {
					//If currWord is empty, return all Types
					autoSuggests = new ArrayList<>(allTypeNames.size());
					autoSuggests.addAll(allTypeNames);
				}
				else {
					//If not empty, filter based on currWord
					autoSuggests = new ArrayList<>();
					for (String typeCode : allTypeNames) {
						if (findMatches(currentPart, typeCode)) {
							autoSuggests.add(typeCode);
						}
					}
				}
			}
			
			boolean endingSemiColon = true;
			if (showAttributes) {
				String typeName = headerParts[0];
				
				String lastAttribute = plugin.getAttributeName(typeName, currentPart);
				if (lastAttribute != null) {
					endingSemiColon = false;
				}
				
				List<String> attributeNames = plugin.getAllAttributeNames(typeName);
				
				if (currentPart.trim().isEmpty() || thisAttrib.endsWith(";") || thisAttrib.endsWith("(") || !endingSemiColon) {
					//If currentPart is empty, return all Attributes not already used
					autoSuggests = new ArrayList<>(attributeNames.size());
					for (String attributeQualifier : attributeNames) {
						if (!thisAttrib.contains(attributeQualifier + ";") 
								&& !thisAttrib.contains(attributeQualifier + "(") 
								&& !thisAttrib.contains(attributeQualifier + "[") 
								|| thisAttrib.endsWith("(")) {
							autoSuggests.add(attributeQualifier);
						}
					}
				}
				else {
					//If not empty, filter based on currentPart
					autoSuggests = new ArrayList<>();
					for (String attributeQualifier : attributeNames) {
						boolean insideBrackets = false;
						if (currentPart.contains("(")) {
							currentPart = currentPart.substring(currentPart.indexOf("(") + 1, currentPart.length());
							insideBrackets = true;
						}
						//If its inside brackets, include all attributes
						//Otherwise, exclude those already used
						
						if (findMatches(attributeQualifier, currentPart)) {
							if (insideBrackets) {
								autoSuggests.add(attributeQualifier);
							}
							else {
								//Dont add already used attribute
								if (!thisAttrib.contains(attributeQualifier + ";") 
								&& !thisAttrib.contains(attributeQualifier + "(") 
								&& !thisAttrib.contains(attributeQualifier + "[")
								|| thisAttrib.endsWith("(")) {
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
				if (currentPart.trim().isEmpty() || thisAttrib.endsWith("[") || thisAttrib.endsWith(",")) {
					if (thisAttrib.endsWith(",")) {
						includesComma = true;
						//already have 1, filter the list
						autoSuggests = new ArrayList<>();
						String keywordName = thisAttrib.substring(thisAttrib.indexOf("[") + 1, thisAttrib.lastIndexOf("="));
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
						if (findMatches(keyword, currentPart)) {
							autoSuggests.add(keyword);
						}
					}
				}
			}
			 
			ICompletionProposal[] autoCompProposals = null;
			
			if (autoSuggests != null && autoSuggests.size() > 0) {
				Collections.sort(autoSuggests);
				
				if (showTypes)
					autoCompProposals = typeProposals(cursorPosition, autoSuggests, thisAttrib, currentPart);
				if (showAttributes)
					autoCompProposals = attributeProposals(cursorPosition, autoSuggests, thisAttrib, currentPart, endingSemiColon);
				if (showKeywords)
					autoCompProposals = keywordProposals(cursorPosition, autoSuggests, thisAttrib, currentPart, includeEquals, includesComma);
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

	private ICompletionProposal[] typeProposals(int cursorPosition, List<String> autoSuggests, String thisAttrib, String currentPart) {
		
		int counter = 0;
		ICompletionProposal[] suggestions = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			
			//Each proposal contains the text to propose, as well as information about where to insert the text into the document. 
			suggestions[counter] = new CompletionProposal(autoSuggest + ";", cursorPosition - thisAttrib.length(), thisAttrib.length(), autoSuggest.length() + 1);
			counter++;
		}
		return suggestions;
	}
	
	private ICompletionProposal[] attributeProposals(int cursorPosition, List<String> autoSuggests, String thisAttrib, String currentPart, boolean endingSemiColon) {
		
		int counter = 0;
		ICompletionProposal[] suggestions = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			
			//Each proposal contains the text to propose, as well as information about where to insert the text into the document.
			if (thisAttrib.endsWith(";") || thisAttrib.endsWith("(") || !endingSemiColon) {
				suggestions[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
			}
			else {
				suggestions[counter] = new CompletionProposal(autoSuggest, cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length());
			}
			counter++;
		}
		return suggestions;
	}
	
	private ICompletionProposal[] keywordProposals(int cursorPosition, List<String> autoSuggests, String thisAttrib, String currentPart, boolean includeEquals, boolean includesComma) {
		
		int counter = 0;
		ICompletionProposal[] suggestions = new ICompletionProposal[autoSuggests.size()];
		for (Iterator<String> iter = autoSuggests.iterator(); iter.hasNext();) {
			String autoSuggest = (String)iter.next();
			
			//Each proposal contains the text to propose, as well as information about where to insert the text into the document.
			if (thisAttrib.endsWith("[")) {
				suggestions[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition, 0, autoSuggest.length() + 1);
			}
			else {
				if (includeEquals) {
					if (includesComma) {
						suggestions[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition, 0, autoSuggest.length() + 1);
					}
					else {
						suggestions[counter] = new CompletionProposal(autoSuggest + "=", cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length() + 1);
					}
				}
				else if (includesComma) {
					if (thisAttrib.endsWith("=")) {
						suggestions[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
					}
					else {
						suggestions[counter] = new CompletionProposal(autoSuggest, cursorPosition - currentPart.length(), currentPart.length(), autoSuggest.length() + 1);
					}
				}
				else {
					suggestions[counter] = new CompletionProposal(autoSuggest, cursorPosition, 0, autoSuggest.length());
				}
			}
			counter++;
		}
		return suggestions;
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
	public IContextInformationValidator getContextInformationValidator() {

		return iciv;
	}

	@Override
	public String getErrorMessage() {

		return exMsg;
	}

}
