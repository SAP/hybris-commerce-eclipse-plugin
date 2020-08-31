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

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class ImpexCommentContentAssistProcessor implements IContentAssistProcessor {

	private IContextInformationValidator iciv;
	private String exMsg = null;
	
	public ImpexCommentContentAssistProcessor() {
		super();
		iciv = new ContextInformationValidator(this);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer textViewer, int documentOffset) {
		
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer textViewer, int documentOffset) {
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
