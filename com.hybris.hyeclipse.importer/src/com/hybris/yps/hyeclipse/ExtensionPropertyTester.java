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
package com.hybris.yps.hyeclipse;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

public class ExtensionPropertyTester extends PropertyTester {
	private static final String PROPERTY_IS_EXTENSION = "isExtension";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals(PROPERTY_IS_EXTENSION)) {
			return testExtension(receiver);
		}
		return false;
	}

	/**
	 * Tests if selected file is an eclipse project
	 * 
	 * @param receiver
	 *            the receiver of the property test
	 * @return true if file is impex file
	 */
	private boolean testExtension(Object receiver) {
		if (receiver instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) receiver;
			if (list.size() == 1 && list.get(0) instanceof IProject) {
				final IProject project = (IProject) list.get(0);
				return project.exists();
			} else if (list.size() == 1 && list.get(0) instanceof IJavaProject) {
				final IJavaProject project = (IJavaProject) list.get(0);
				return project.exists();
			}			
		}
		return false;
	}

}
