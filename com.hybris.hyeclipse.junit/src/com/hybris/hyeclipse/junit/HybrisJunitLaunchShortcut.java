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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

@SuppressWarnings("restriction")
public class HybrisJunitLaunchShortcut extends JUnitLaunchShortcut
{
	   @Override
	   protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement pElement) throws CoreException
	   {
	      ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(pElement);
	      config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, "com.hybris.junit.kind");
	      return config;
	   }
	}
