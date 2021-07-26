package com.sap.cc.lsp.util;

public abstract class DocumentLine {
	private int line;
	private String text;
	private int charOffset;
	

	public DocumentLine(int line, String text, int charOffset) {
		super();
		this.line = line;
		this.text = text;
		this.charOffset = charOffset;
	}
	
	public int getLine() {
		return line;
	}
	public void setLine(int line) {
		this.line = line;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getCharOffset() {
		return charOffset;
	}
	public void setCharOffset(int charOffset) {
		this.charOffset = charOffset;
	}
}