package com.hybris.impexformatter.utils;

import org.eclipse.swt.graphics.RGB;

public class ColorHelper {

	public static final String rgbTohex(RGB color) {
		Integer value = color.blue | (color.green << 8) | (color.red << 16);
		return '#' + Integer.toHexString(value);
	}
	
	public static final RGB hexTorgb(String color) {
		java.awt.Color result = java.awt.Color.decode(color);
		RGB rgb = new RGB(result.getRed(), result.getGreen(), result.getBlue());
		return rgb;
	}
}
