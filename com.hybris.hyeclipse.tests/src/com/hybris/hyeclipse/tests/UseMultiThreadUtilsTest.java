package com.hybris.hyeclipse.tests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.hybris.yps.hyeclipse.utils.UseMultiThreadUtils;

public class UseMultiThreadUtilsTest {
	
	private static final String RESOURCES_BIN_PLATFORM = "resources/bin/platform";
	private static final String SERVER_XML_PATH = "resources/config/tomcat/conf/server.xml";
	
	UseMultiThreadUtils testObj;
	Path copyxml = null;
	
	
	@Before
	public void before() throws IOException {
		testObj = new UseMultiThreadUtils();
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
		
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbFactory.newDocumentBuilder();
		Document doc = db.parse(serverxml.toFile());
	}
	
	@After
	public void tearDown() throws IOException {
		// copy back original file
		Path cp = java.nio.file.Paths.get("").toAbsolutePath();
		Path serverxml = cp.resolve(SERVER_XML_PATH);
		Files.copy(copyxml, serverxml, StandardCopyOption.REPLACE_EXISTING);
		Assert.assertTrue(Files.readAllLines(copyxml)
			      .equals(Files.readAllLines(serverxml)));
		Files.delete(copyxml);
	}

}
