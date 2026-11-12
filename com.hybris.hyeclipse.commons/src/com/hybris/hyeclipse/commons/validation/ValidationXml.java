/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.hyeclipse.commons.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Secure XML parsing with source lines and the existing TSV report shape, without JAXB. */
public final class ValidationXml {
	private static final String LINE_KEY = "tsv.source.line";

	private ValidationXml() { /* intentionally empty */ }

	/**
	 * @param input caller-owned input
	 * @param monitor cancellation monitor
	 * @return DOM with source line information
	 * @throws IOException if parsing fails; external entities and DTDs are rejected
	 */
	public static Document parse(final InputStream input, final IProgressMonitor monitor) throws IOException {
		try {
			final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			final Document document = domFactory.newDocumentBuilder().newDocument();
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			final SAXParser parser = factory.newSAXParser();
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			parser.parse(input, new DefaultHandler() {
				private Locator locator;
				private Node current = document;
				@Override
				public void setDocumentLocator(final Locator value) { locator = value; }
				@Override
				public void startElement(final String uri, final String local, final String name,
						final Attributes attributes) {
					checkCanceled(monitor);
					final Element element = document.createElementNS(uri.isEmpty() ? null : uri, name);
					element.setUserData(LINE_KEY, locator.getLineNumber(), null);
					for (int i = 0; i < attributes.getLength(); i++) {
						element.setAttribute(attributes.getQName(i), attributes.getValue(i));
					}
					current.appendChild(element);
					current = element;
				}
				@Override
				public void endElement(final String uri, final String local, final String name) {
					current = current.getParentNode();
				}
				@Override
				public void characters(final char[] chars, final int offset, final int length) {
					checkCanceled(monitor);
					current.appendChild(document.createTextNode(new String(chars, offset, length)));
				}
				@Override
				public void error(final SAXParseException failure) throws SAXException { throw failure; }
				@Override
				public void fatalError(final SAXParseException failure) throws SAXException { throw failure; }
			});
			return document;
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException("Could not parse validation XML", e);
		}
	}

	/**
	 * @param node XML node
	 * @return original source line
	 */
	public static int line(final Node node) {
		final Object line = node.getUserData(LINE_KEY);
		return line instanceof Integer ? ((Integer) line).intValue() : 1;
	}

	static List<Element> children(final Element parent, final String name) {
		final List<Element> result = new ArrayList<>();
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element && name.equals(node.getNodeName())) {
				result.add((Element) node);
			}
		}
		return result;
	}

	static Element child(final Element parent, final String name) {
		final List<Element> values = children(parent, name);
		return values.isEmpty() ? null : values.get(0);
	}

	static void checkCanceled(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	/**
	 * @param results validation findings
	 * @return XML consumable by the TSV editor
	 */
	public static String report(final List<ValidationResult> results) {
		final StringWriter output = new StringWriter();
		try {
			final XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
			try {
				xml.writeStartDocument("UTF-8", "1.0");
				xml.writeStartElement("validation_results");
				for (final ValidationResult result : results) {
					if (result.getState() == ValidationResult.State.PASS) {
						continue;
					}
					xml.writeStartElement("result");
					value(xml, "filename", result.getFile().getAbsolutePath());
					value(xml, "line", Integer.toString(result.getLine()));
					value(xml, "rule_id", result.getRuleId());
					value(xml, "rule_priority", result.getPriority());
					value(xml, "state", result.getState().name());
					value(xml, "element", result.getElement());
					value(xml, "description", result.getDescription());
					xml.writeEndElement();
				}
				xml.writeEndElement();
				xml.writeEndDocument();
			} finally { xml.close(); }
			return output.toString();
		} catch (XMLStreamException e) { throw new IllegalStateException("Could not serialize validation results", e); }
	}

	private static void value(final XMLStreamWriter xml, final String name, final String value)
			throws XMLStreamException {
		xml.writeStartElement(name);
		xml.writeCharacters(value);
		xml.writeEndElement();
	}
}
