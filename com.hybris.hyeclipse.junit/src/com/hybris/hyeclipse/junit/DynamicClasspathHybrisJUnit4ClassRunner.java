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
package com.hybris.hyeclipse.junit;


import org.junit.runners.model.InitializationError;

import de.hybris.platform.testframework.HybrisJUnit4ClassRunner;


/**
 * Compatibility runner. Dependencies are supplied by the launch classpath provider,
 * not by reflective mutation of the system classloader (unsupported since Java 9).
 */
public class DynamicClasspathHybrisJUnit4ClassRunner extends HybrisJUnit4ClassRunner {

	/**
	 * @param clazz integration test class
	 * @throws InitializationError if the Commerce test framework cannot initialize
	 */
	public DynamicClasspathHybrisJUnit4ClassRunner(final Class<?> clazz) throws InitializationError {
		super(clazz);
	}
}
