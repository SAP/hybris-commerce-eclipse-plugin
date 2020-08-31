/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
