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
package com.hybris.hyeclipse.tsv.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hybris.ps.tsv.results.IResult;
import com.hybris.ps.tsv.results.IResultFactory;
import com.hybris.ps.tsv.results.ResultState;
import com.hybris.ps.tsv.rules.IRule;
import com.hybris.ps.tsv.rules.IRuleSet;

public class ItemsXmlValidator {
	
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	
	private final SAXParser sp;
    
	private final IRuleSet ruleSet;
	private final IResultFactory resultFactory;
	
	public ItemsXmlValidator(final IRuleSet ruleSet, final IResultFactory resultFactory) throws ParserConfigurationException, SAXException {
		final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        this.sp = factory.newSAXParser();
        sp.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
		
        this.ruleSet = ruleSet;
		this.resultFactory = resultFactory;
	}
	
	private Document parseItemsXml(final InputStream is, final IProgressMonitor monitor) throws ParserConfigurationException, SAXException, IOException {
        final Document doc = DocumentBuilderFactory.
                newInstance().
                newDocumentBuilder().
                newDocument();

        sp.parse(is, new LocationRecordingProgressReportingHandler(doc, monitor));
        
        return doc;
	}
	
	public List<IResult> analyze(final File itemsXmlLocation, final IProgressMonitor monitor) {
		final List<IResult> results = new LinkedList<>();
		try {
	        final SubMonitor progress = SubMonitor.convert(monitor, 100);
			
	        try (final InputStream is = new FileInputStream(itemsXmlLocation)) {
				final Document doc = parseItemsXml(is, progress.newChild(20));
				final SubMonitor ruleCheckMonitor = progress.newChild(80).setWorkRemaining(ruleSet.getRules().size());
				for (final IRule rule : ruleSet.getRules()) {
					results.addAll(rule.check(itemsXmlLocation, doc));
					ruleCheckMonitor.worked(1);
				}
	        }

		}
		catch (SAXParseException e) {
			results.add(resultFactory.createResult(itemsXmlLocation, e.getLineNumber(), null, null, "Error parsing items XML", ResultState.ERROR));
		}
		catch (FileNotFoundException e) {
			results.add(resultFactory.createError("Items XML file not found: " + itemsXmlLocation.getAbsolutePath()));
		}
		catch (ParserConfigurationException | SAXException | IOException e) {
			results.add(resultFactory.createError("Failed to process items.xml"));
		}
		return results;
	}
	
	public List<IResult> analyze(final IFile itemsXmlFile, final IProgressMonitor monitor) {
		return analyze(itemsXmlFile.getLocation().toFile(), monitor);
	}

}
