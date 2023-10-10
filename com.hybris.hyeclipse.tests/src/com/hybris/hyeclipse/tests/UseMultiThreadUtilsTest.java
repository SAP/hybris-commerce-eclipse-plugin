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
package com.hybris.hyeclipse.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hybris.hyeclipse.commons.utils.XmlScannerUtils;
import com.hybris.yps.hyeclipse.utils.UseMultiThreadUtils;

public class UseMultiThreadUtilsTest {
	
	private static final String RESOURCES_BIN_PLATFORM = "resources/bin/platform";
	private static final String SERVER_XML_PATH = "resources/config/tomcat/conf/server.xml";
	
	Path copyxml = null;
	
	
	@Before
	public void before() throws IOException {
		Path cp = java.nio.file.Paths.get("").toAbsolutePath();
		Path serverxml = cp.resolve(SERVER_XML_PATH);
		UUID value = UUID.randomUUID();
		String copyTo = SERVER_XML_PATH.concat(value.toString());
		copyxml = Paths.get(copyTo);
		Files.copy(serverxml, copyxml, StandardCopyOption.REPLACE_EXISTING);
		
	}

	@Test
	public void testUseMultiThread() throws ParserConfigurationException, SAXException, IOException {
		
		Path cp = java.nio.file.Paths.get("").toAbsolutePath();
		Path yplatform = cp.resolve(RESOURCES_BIN_PLATFORM);
		Path serverxml = cp.resolve(SERVER_XML_PATH);
		
		UseMultiThreadUtils.useMultiThread(yplatform.toFile());
		DocumentBuilder db = XmlScannerUtils.newDocumentBuilder();
		Document doc = db.parse(serverxml.toFile());
		
		NodeList nodes = doc.getElementsByTagName(UseMultiThreadUtils.FIND_TAG_NODE);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			NamedNodeMap attrs = n.getAttributes();
			Node ss = attrs.getNamedItem(UseMultiThreadUtils.SS_ATTRIBUTE);
			if (ss != null) {
				Assert.assertEquals("value in XML should have value '0'", UseMultiThreadUtils.SS_RESET_VALUE, ss.getTextContent());
			}
		}
		
		
	}
	
	@After
	public void tearDown() throws IOException {
		// copy back original file
		Path cp = java.nio.file.Paths.get("").toAbsolutePath();
		Path serverxml = cp.resolve(SERVER_XML_PATH);
		Files.copy(copyxml, serverxml, StandardCopyOption.REPLACE_EXISTING);
		Assert.assertEquals(Files.readAllLines(copyxml), Files.readAllLines(serverxml));
		Files.delete(copyxml);
	}

}
