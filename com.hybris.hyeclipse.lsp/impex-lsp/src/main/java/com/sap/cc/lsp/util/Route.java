package com.sap.cc.lsp.util;

public class Route extends DocumentLine {
    private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Route(int line, int charOffset, String text, String name) {
		super(line, text, charOffset);
		this.name = name;
	}

	
	
    
    
}
