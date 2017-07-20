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