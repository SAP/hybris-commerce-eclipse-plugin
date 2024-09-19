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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.ImpexDocumentPartitioner;
import com.hybris.impexformatter.assistprocessors.ImpexCommandContentAssistProcessor;
import com.hybris.impexformatter.assistprocessors.ImpexCommentContentAssistProcessor;
import com.hybris.impexformatter.assistprocessors.ImpexDataContentAssistProcessor;
import com.hybris.impexformatter.assistprocessors.ImpexInstructionContentAssistProcessor;
import com.hybris.impexformatter.assistprocessors.ImpexTypeSystemContentAssistProcessor;
import com.hybris.impexformatter.hyperlink.ImpexTypeHyperlinkDetector;
import com.hybris.impexformatter.scanners.ImpexBracesAutoEditStrategy;
import com.hybris.impexformatter.scanners.ImpexRuleScanner;
import com.hybris.impexformatter.scanners.InstructionsRuleScanner;

public class ImpexSourceViewerConfig extends TextSourceViewerConfiguration {
	
	Map<String, RuleBasedScanner> scanMap = new HashMap<>();

	public ImpexSourceViewerConfig(IPreferenceStore preferenceStore) {
		
		super(preferenceStore);
	}
	
	@Override 
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) { 

		return new IHyperlinkDetector[]{new ImpexTypeHyperlinkDetector()}; 
	}
	
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		
		PresentationReconciler reconciler = new PresentationReconciler();
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getImpexScanner(ImpexDocumentPartitioner.IMPEX_INSTRUCTION));
		reconciler.setDamager(dr, ImpexDocumentPartitioner.IMPEX_INSTRUCTION);
		reconciler.setRepairer(dr, ImpexDocumentPartitioner.IMPEX_INSTRUCTION);
		dr = new DefaultDamagerRepairer(getImpexScanner(null));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		
		return Activator.IMPEX_PARTITIONING;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		
		return new String[] { 
				IDocument.DEFAULT_CONTENT_TYPE,
				ImpexDocumentPartitioner.IMPEX_COMMENT,
				ImpexDocumentPartitioner.IMPEX_DATA,
				ImpexDocumentPartitioner.IMPEX_HEADER,
				ImpexDocumentPartitioner.IMPEX_INSTRUCTION };
	}
	
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        
		IAutoEditStrategy strategy= (IDocument.DEFAULT_CONTENT_TYPE.equalsIgnoreCase(contentType) ? new ImpexBracesAutoEditStrategy() : new DefaultIndentLineAutoEditStrategy());
        
		IAutoEditStrategy headerStr = (ImpexDocumentPartitioner.IMPEX_HEADER.equalsIgnoreCase(contentType) ? new ImpexBracesAutoEditStrategy() : new DefaultIndentLineAutoEditStrategy());
        
		return new IAutoEditStrategy[] { strategy, headerStr };
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		
		ContentAssistant assistant = new ContentAssistant();
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(300);
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		assistant.enableAutoInsert(true);
		assistant.enablePrefixCompletion(true);
		
		IContentAssistProcessor pr = new ImpexInstructionContentAssistProcessor();
		assistant.setContentAssistProcessor(pr, ImpexDocumentPartitioner.IMPEX_INSTRUCTION);
		
		//pr = new ImpexTypeAttributeContentAssistProcessor();
		pr = new ImpexTypeSystemContentAssistProcessor();
		assistant.setContentAssistProcessor(pr, ImpexDocumentPartitioner.IMPEX_HEADER);
		
		pr = new ImpexDataContentAssistProcessor();
		assistant.setContentAssistProcessor(pr, ImpexDocumentPartitioner.IMPEX_DATA);
		
		pr = new ImpexCommentContentAssistProcessor();
		assistant.setContentAssistProcessor(pr, ImpexDocumentPartitioner.IMPEX_COMMENT);
		
		pr = new ImpexCommandContentAssistProcessor();
		assistant.setContentAssistProcessor(pr, IDocument.DEFAULT_CONTENT_TYPE);
		
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return assistant;
	}

	protected RuleBasedScanner getImpexScanner(String partition) {
		
		RuleBasedScanner scanner = scanMap.get(partition); 
		if (scanner == null) {
			scanner = new ImpexRuleScanner(ColorProvider.getInstance());
			if (partition != null) {
				switch (partition) {
				case ImpexDocumentPartitioner.IMPEX_INSTRUCTION:
					scanner = new InstructionsRuleScanner(ColorProvider.getInstance());
					break;
				}
			}
			
		}
		return scanner;
	}
	
}
