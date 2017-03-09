package com.hybris.hyeclipse.tsvextended.utils;

import de.hybris.bootstrap.config.ExtensionInfo;

public class ExtensionHolder {
	
	private String name;
	private ExtensionInfo extensionInfo;
	private boolean isSelected = false;

	public ExtensionHolder(ExtensionInfo extensionInfo) {
		this.extensionInfo = extensionInfo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExtensionInfo getExtensionInfo() {
		return extensionInfo;
	}

	public void setExtensionInfo(ExtensionInfo extensionInfo) {
		this.extensionInfo = extensionInfo;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

}
