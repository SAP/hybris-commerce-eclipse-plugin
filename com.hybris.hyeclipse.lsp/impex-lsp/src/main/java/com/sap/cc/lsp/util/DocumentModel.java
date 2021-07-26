package com.sap.cc.lsp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;


public class DocumentModel {

	
	public final List<DocumentLine> lines = new ArrayList<>();
	public final List<Route> routes = new ArrayList<>();
	public final Map<String, VariableDefinition> variables = new HashMap<>();
	
	public DocumentModel(String text) {
		try (
			Reader r = new StringReader(text);
			BufferedReader reader = new BufferedReader(r);
		) {
			String lineText;
			int lineNumber = 0;
			while ((lineText = reader.readLine()) != null) {
				DocumentLine line = null;
				// TODO: languge syntax change
				/*if (line.startsWith("#")) {
					continue;
				}*/
				if (lineText.contains("=")) {
					line = variableDefinition(lineNumber, lineText);
				} else if (!lineText.trim().isEmpty()) {
					Route route = new Route(lineNumber, 0, lineText, resolve(lineText));
					routes.add(route);
					line = route;
				}
				if (line != null) {
					lines.add(line);
				}
				lineNumber++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String resolve(String line) {
		for (Entry<String, VariableDefinition> variable : variables.entrySet()) {
			line = line.replace("${" + variable.getKey() + "}", variable.getValue().getVariableValue());
		}
		return line;
	}

	private VariableDefinition variableDefinition(int lineNumber, String line) {
		String[] segments = line.split("=");
		if (segments.length == 2) {
			VariableDefinition def = new VariableDefinition(lineNumber, 0, line, segments[0], segments[1]);
			variables.put(def.getVariableName(), def);
			return def;
		}
		return null;
	}

	public List<Route> getResolvedRoutes() {
		return Collections.unmodifiableList(this.routes);
	}

	public String getVariable(int lineNumber, int character) {
		Optional<DocumentLine> docLine = this.lines.stream().filter(line -> line.getLine() == lineNumber).findFirst();
		if (!docLine.isPresent()) {
			return null;
		}
		String text = docLine.get().getText();
		if (text.contains("=") && character < text.indexOf("=")) {
			return text.split("=")[0];
		}
		int prefix = text.substring(0, character).lastIndexOf("${");
		int suffix = text.indexOf("}", character);
		if (prefix >= 0 && suffix >= 0) {
			return text.substring(prefix + "${".length(), suffix);
		}
		return null;
	}
	
	public Route getRoute(int line) {
		for (Route route : getResolvedRoutes()) {
			if (route.getLine() == line) {
				return route;
			}
		}
		return null;
	}

	public int getDefintionLine(String variable) {
		if (this.variables.containsKey(variable)) {
			return this.variables.get(variable).getLine();
		}
		return -1;
	}

	public List<DocumentLine> getResolvedLines() {
		return Collections.unmodifiableList(this.lines);
	}

}
