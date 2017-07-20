package com.hybris.hyeclipse.junit;

import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;


public class HybrisJUnitRequest extends Request{

	private Class<?> clazz;

	public HybrisJUnitRequest(final Class<?> clazz)
	{
		super();
		this.clazz = clazz;
	}

	@Override
	public Runner getRunner()
	{
		Runner runner = null;
		try
		{
			runner = new DynamicClasspathHybrisJUnit4ClassRunner(clazz);
		}
		catch (InitializationError initializationError)
		{
			initializationError.printStackTrace();
			throw new RuntimeException(initializationError);
		}

		return runner;
	}

}
