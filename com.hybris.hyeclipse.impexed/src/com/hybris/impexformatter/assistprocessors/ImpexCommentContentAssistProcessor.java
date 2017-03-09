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
