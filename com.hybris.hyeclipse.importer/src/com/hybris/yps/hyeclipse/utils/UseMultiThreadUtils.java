package com.hybris.yps.hyeclipse.utils;


import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Created by Qiang Zeng on 26/06/2017.
 */
public class UseMultiThreadUtils
{
	public static final String TOMCAT_SERVER_XML_FILE = "tomcat" + File.separator + "conf" + File.separator + "server.xml";

	public static void useMultiThread(File platformHome)
	{
		String fileFullPath = platformHome.getAbsolutePath() + File.separator + ".." + File.separator + ".." + File.separator + "config" + File.separator + TOMCAT_SERVER_XML_FILE;
		File serverXmlFile = new File(fileFullPath);
		if (serverXmlFile.exists())
		{
			try
			{
				String content = new String(Files.readAllBytes(Paths.get(serverXmlFile.getAbsolutePath())));
				String toRemove1 = "startStopThreads=\"[\\d]*\"";
				String toRemove2 = "startStopThreads=\'[\\d]*\'";
				String toReplace = "<Host";
				String replacement = "<Host startStopThreads=\"0\"";
				//The removal is to avoid adding duplicate configurations in case it was already manually configured
				content = content.replaceAll(toRemove1, "");
				content = content.replaceAll(toRemove2, "");
				content = content.replaceAll(toReplace, replacement);

				PrintWriter writer = new PrintWriter(serverXmlFile, "UTF-8");
				writer.println(content);
				writer.close();
			}
			catch (Exception e)
			{
				throw new IllegalStateException("Failed to access the server.xml file at: " + fileFullPath);
			}
		}
		else
		{
			throw new IllegalStateException(fileFullPath + " doesn't exist.");
		}
	}
}
