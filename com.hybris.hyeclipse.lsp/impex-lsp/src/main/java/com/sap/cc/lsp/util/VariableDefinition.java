/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sap.cc.lsp.util;

/**
 *
 * @author i303764
 */
public class VariableDefinition extends DocumentLine {
	private String variableName;
	private String variableValue;

	public VariableDefinition(int lineNumber, int charOffset, String text, String variableName, String variableValue) {
		super(lineNumber, text, charOffset);
		this.variableName = variableName;
		this.variableValue = variableValue;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableValue() {
		return variableValue;
	}

	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}
}
