///*******************************************************************************
// * Copyright 2020 SAP
// * 
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License.  You may obtain a copy
// * of the License at
// * 
// *   http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// * License for the specific language governing permissions and limitations under
// * the License.
// ******************************************************************************/
//package com.hybris.hyeclipse.junit;
//
//import org.junit.runner.Request;
//import org.junit.runner.Runner;
//import org.junit.runners.model.InitializationError;
//
//
//public class HybrisJUnitRequest extends Request{
//
//	private Class<?> clazz;
//
//	public HybrisJUnitRequest(final Class<?> clazz)
//	{
//		super();
//		this.clazz = clazz;
//	}
//
//	@Override
//	public Runner getRunner()
//	{
//		Runner runner = null;
//		try
//		{
//			runner = new DynamicClasspathHybrisJUnit4ClassRunner(clazz);
//		}
//		catch (InitializationError initializationError)
//		{
//			initializationError.printStackTrace();
//			throw new RuntimeException(initializationError);
//		}
//
//		return runner;
//	}
//
//}
