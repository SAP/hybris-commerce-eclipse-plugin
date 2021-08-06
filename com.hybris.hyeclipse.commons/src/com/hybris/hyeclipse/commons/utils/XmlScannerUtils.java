package com.hybris.hyeclipse.commons.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlScannerUtils { 
	
	
	private XmlScannerUtils() {
		throw new IllegalStateException("utility class");
	}

	/**
	 * Scans {@code localextensions.xml} file and return set of declared extensions in project. Set is flat, meaning, that does not include top tier extension dependencies.
	 * @param localExtensionPath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public static Set<String> getLocalExtensions(java.nio.file.Path localExtensionPath)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		DocumentBuilder db = newDocumentBuilder();

		Document document = db.parse(new FileInputStream(localExtensionPath.toFile()));

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		NodeList userElements = (NodeList) xpath.evaluate("//extension/@name", document, XPathConstants.NODESET);

		Set<String> extensionSet = new HashSet<>();

		for (int i = 0; i < userElements.getLength(); i++) {
			extensionSet.add(userElements.item(i).getNodeValue().toLowerCase());
		}
		return extensionSet;
	}

	/**
	 * Builds XML {@link DocumentBuilder} with patched security checks.
	 * @return secured {@link DocumentBuilder} instance.
	 * @throws ParserConfigurationException
	 */
	public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// to be compliant, completely disable DOCTYPE declaration:
		dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		// or completely disable external entities declarations:
		dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
		dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		// or prohibit the use of all protocols by external entities:
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return dbf.newDocumentBuilder();
	}
	
	/**
	 * Builds new {@link Transformer} with security patches.
	 * @return new {@link Transformer} with security patches.
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 */
	public static Transformer newTransformer()
			throws TransformerFactoryConfigurationError, TransformerConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
//		to be compliant prohibit the use of all protocols by external entities:
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		return transformerFactory.newTransformer();

	}

}
