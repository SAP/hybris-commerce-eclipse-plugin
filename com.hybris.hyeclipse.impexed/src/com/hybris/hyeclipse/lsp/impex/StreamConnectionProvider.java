package com.hybris.hyeclipse.lsp.impex;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class StreamConnectionProvider extends ProcessStreamConnectionProvider implements org.eclipse.lsp4e.server.StreamConnectionProvider {

	public StreamConnectionProvider() { 

        List<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add("/Users/i303764/SAPDevelop/0/hybris-commerce-eclipse-plugin/com.hybris.hyeclipse.lsp/impex-lsp/target/impex-lsp-1.5.6-SNAPSHOT-spring-boot.jar"); 
        setCommands(commands);
        setWorkingDirectory(System.getProperty("user.dir"));
		
	}
}
