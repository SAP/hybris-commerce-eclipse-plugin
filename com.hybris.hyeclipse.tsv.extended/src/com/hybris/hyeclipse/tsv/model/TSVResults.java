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
package com.hybris.hyeclipse.tsv.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hybris.hyeclipse.commons.validation.ValidationXml;
public class TSVResults {

	public TSVResults(){}
	
	private Set<TSVResult> results;
	
	public Set<TSVResult> getResults() {
		return results;
	}
	public void setResults(Set<TSVResult> results) {
		this.results = results;
	}

	/**
	 * @param input caller-owned report stream
	 * @return parsed TSV report
	 * @throws IOException if the report is invalid
	 */
	public static TSVResults read(final InputStream input) throws IOException {
		final Document document = ValidationXml.parse(input, new NullProgressMonitor());
		if (!"validation_results".equals(document.getDocumentElement().getTagName())) {
			throw new IOException("Expected a TSV validation_results report");
		}
		final Set<TSVResult> rows = new LinkedHashSet<>();
		final NodeList nodes = document.getElementsByTagName("result");
		for (int i = 0; i < nodes.getLength(); i++) {
			final Element node = (Element) nodes.item(i);
			final TSVResult result = new TSVResult();
			result.setFilename(text(node, "filename"));
			try { result.setLineNumber(Integer.parseInt(text(node, "line"))); }
			catch (NumberFormatException e) { throw new IOException("Invalid result source line", e); }
			result.setRuleId(text(node, "rule_id"));
			result.setRulePriority(text(node, "rule_priority"));
			result.setState(text(node, "state"));
			result.setElement(text(node, "element"));
			result.setDescription(text(node, "description"));
			rows.add(result);
		}
		final TSVResults report = new TSVResults();
		report.setResults(rows);
		return report;
	}

	private static String text(final Element node, final String name) throws IOException {
		final NodeList values = node.getElementsByTagName(name);
		if (values.getLength() != 1) {
			throw new IOException("Missing or duplicate result field: " + name);
		}
		return values.item(0).getTextContent();
	}
	
}
