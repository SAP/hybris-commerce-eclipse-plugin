package com.hybris.impexformatter.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.Maps;

public class ColorProvider {
	
	private static class ColorProviderHolder { 
        private final static ColorProvider instance = new ColorProvider();
    }
	
	public static ColorProvider getInstance() {
		return ColorProviderHolder.instance;
	}
	
	private ColorProvider() {
		rgbPattern = Pattern.compile(pattern);
	}
	
	
	public static final RGB BLUE_COLOR = new RGB(0, 0, 255);
	public static final RGB GREY_COLOR = new RGB(192, 192, 192);
	public static final RGB DGREY_COLOR = new RGB(180, 180, 180);
	public static final RGB ORANGE_COLOR = new RGB(255, 128, 0);
	public static final RGB PURPLE_COLOR = new RGB(163, 73, 164);
	public static final RGB RED_COLOR = new RGB(204, 0, 0);
	public static final RGB BLACK_COLOR = new RGB(0, 0, 0);
	public static final String pattern = "RGB \\{([0-9]{1,3}),[ ]?([0-9]{1,3}),[ ]?([0-9]{1,3})\\}";
	public static final RGB GREEN_COLOR = new RGB(0, 255, 0);


	protected Map<RGB, Color> fColorTable = new HashMap<>(10);
	protected Map<Integer, RGB> fRgbMap = Maps.newHashMap();
	
	private Pattern rgbPattern;


	public void dispose() {
		Iterator<Color> e = fColorTable.values().iterator();
		while (e.hasNext())
			((Color) e.next()).dispose();
	}

	public Color getColor(RGB rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
	
	
	/**
	 * if string is in format "RGB {255,255,255}" will be transformed to {@code Color} object.
	 * @param serialFormat
	 * @return color object
	 */
	public Color getColor(String serialFormat) {
		Matcher matcher = rgbPattern.matcher(serialFormat);
		if (!matcher.matches()) {
			throw new PatternSyntaxException("serial format is in wrong format " + serialFormat, pattern, 0);
		}
		Integer red = Integer.valueOf(matcher.group(1));
		Integer green = Integer.valueOf(matcher.group(2));
		Integer blue = Integer.valueOf(matcher.group(3));
		RGB rgb = new RGB(red, green, blue);
		return getColor(rgb);
	}
	
	/**
	 * if parameters contain color value then method return {@inheritDoc Color} object.
	 * @param r red value for RGB color
	 * @param g green value for RGB color
	 * @param b blue value for RGB color
	 * 
	 * @return color object
	 */
	public Color getColor(final int r, final int g,final int b) {
		RGB rgb = new RGB(r, g, b);
		return getColor(rgb);
	}
}
