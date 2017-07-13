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
	   protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement p_element) throws CoreException
	   {
	      ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(p_element);
	      config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, "com.hybris.junit.kind");
	      return config;
	   }
	}
