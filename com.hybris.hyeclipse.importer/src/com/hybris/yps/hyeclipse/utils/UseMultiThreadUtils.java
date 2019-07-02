package com.hybris.yps.hyeclipse.utils;


import java.io.File;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Created by Qiang Zeng on 26/06/2017.
 */
public class UseMultiThreadUtils
{
	public static final String TOMCAT_SERVER_XML_PATH = "../../config/tomcat/conf/server.xml";
	public static final String FIND_TAG_NODE = "Host";
	public static final String SS_ATTRIBUTE = "startStopThreads";
	public static final String SS_RESET_VALUE = "0";

	public static void useMultiThread(File platformHome)
	{
		
		Path p = platformHome.toPath();
		Path serverxml = p.resolve(TOMCAT_SERVER_XML_PATH).toAbsolutePath();
		
		final File serverFile = serverxml.toFile();
		if (serverFile.exists())
		{
			try
			{
				boolean isDirty = false;
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbFactory.newDocumentBuilder();
				Document doc = db.parse(serverFile);
				NodeList nodes = doc.getElementsByTagName(FIND_TAG_NODE);
				for (int i = 0; i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					NamedNodeMap attrs = n.getAttributes();
					Node ss = attrs.getNamedItem(SS_ATTRIBUTE);
					if (ss != null) {
						ss.setTextContent(SS_RESET_VALUE);
						isDirty = true;
					}
				}
				if (isDirty) {
					// save change
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(serverFile);
					transformer.transform(source, result);
				}
				
			}
			catch (Exception e)
			{
				throw new IllegalStateException(String.format("Failed to access the server.xml file at: %s", serverFile), e);
			}
		}
		else
		{
			throw new IllegalStateException(String.format("%s doesn't exist.", serverFile));
		}
	}
}
