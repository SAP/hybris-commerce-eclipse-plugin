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
package com.hybris.hyeclipse.editor.preferences;

/**
 * Class holding constants for copyright preference page
 */
public final class CopyrightPreferenceConstants {

	public static final String COPYRIGHT_CONTENT = "copyright_content";
	public static final String COPYRIGHT_FIRST_LINE = "copyright_first_line";
	public static final String COPYRIGHT_LINE_PREFIX = "copyright_line_prefix";
	public static final String COPYRIGHT_LAST_LINE = "copyright_last_line";
	public static final String DEFAULT_COPYRIGHT_CONTENT = "Put your copyright text here";
	public static final String DEFAULT_COPYRIGHT_FIRST_LINE = "/*";
	public static final String DEFAULT_COPYRIGHT_LINE_PREFIX = " * ";
	public static final String DEFAULT_COPYRIGHT_LAST_LINE = " */";

	private CopyrightPreferenceConstants() {
		// intentionally empty
	}
}
