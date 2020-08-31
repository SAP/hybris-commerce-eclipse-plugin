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

import org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference;
import org.junit.runner.Description;
import org.junit.runner.Runner;

@SuppressWarnings("restriction")
public class HybrisJUnitTestReference extends JUnit4TestReference
{
	public HybrisJUnitTestReference(Runner runner, Description root) {
		super(new HybrisJUnitRequest(runner.getDescription().getTestClass()).getRunner(), root);
	}
    
}
