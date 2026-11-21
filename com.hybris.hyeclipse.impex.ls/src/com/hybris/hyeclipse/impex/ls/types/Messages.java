/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.ls.types;

import org.eclipse.osgi.util.NLS;

final class Messages extends NLS {
	public static String operation;
	public static String macro;
	public static String headerModifier;
	public static String columnModifier;
	public static String modifierValue;

	static {
		NLS.initializeMessages("com.hybris.hyeclipse.impex.ls.types.Messages", Messages.class); //$NON-NLS-1$
	}

	private Messages() {
	}
}
