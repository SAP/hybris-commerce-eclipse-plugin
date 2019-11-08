package com.hybris.hyeclipse.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.eclipse.jdt.internal.junit.runner.ITestLoader;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader;
import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestReference;
import org.eclipse.jdt.internal.junit4.runner.DescriptionMatcher;
import org.eclipse.jdt.internal.junit4.runner.FailuresFirstSorter;
import org.eclipse.jdt.internal.junit4.runner.SubForestFilter;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;

import junit.framework.Test;

@SuppressWarnings("restriction")
public class HybrisJUnitIntegrationTestLoader implements ITestLoader {

	public ITestReference[] loadTests(
			@SuppressWarnings("rawtypes") 
			Class[] testClasses,
			String testName,
			String[] failureNames,
			RemoteTestRunner listener) {

		ITestReference[] refs= new ITestReference[testClasses.length];
		for (int i= 0; i < testClasses.length; i++) {
			Class<?> clazz= testClasses[i];
			ITestReference ref= createTest(clazz, testName, failureNames, listener);
			refs[i]= ref;
		}
		return refs;
	}

	private Description getRootDescription(Runner runner, DescriptionMatcher matcher) {
		Description current= runner.getDescription();
		while (true) {
			List<Description> children= current.getChildren();
			if (children.size() != 1 || matcher.matches(current))
				return current;
			current= children.get(0);
		}
	}

	private ITestReference createTest(Class<?> clazz, String testName, String[] failureNames, RemoteTestRunner listener) {
		if (clazz == null)
			return null;
		if (testName != null && isJUnit3SetUpTest(clazz, testName)) {
			JUnit3TestLoader jUnit3TestLoader= new JUnit3TestLoader();
			Test test= jUnit3TestLoader.getTest(clazz, testName, listener);
			return new JUnit3TestReference(test);
		}
		if (testName != null) {
			return createFilteredTest(clazz, testName, failureNames);
		}
		return createUnfilteredTest(clazz, failureNames);
	}

	private ITestReference createFilteredTest(Class<?> clazz, String testName, String[] failureNames) {
		DescriptionMatcher matcher= DescriptionMatcher.create(clazz, testName);
		SubForestFilter filter= new SubForestFilter(matcher);
		Request request= sortByFailures(Request.classWithoutSuiteMethod(clazz).filterWith(filter), failureNames);
		Runner runner= request.getRunner();
		Description description= getRootDescription(runner, matcher);
		return new HybrisJUnitTestReference(runner, description);
	}

	private ITestReference createUnfilteredTest(Class<?> clazz, String[] failureNames) {
		Request request= sortByFailures(Request.aClass(clazz), failureNames);
		Runner runner= request.getRunner();
		Description description= runner.getDescription();
		return new HybrisJUnitTestReference(runner, description);
	}

	private Request sortByFailures(Request request, String[] failureNames) {
		if (failureNames != null) {
			return request.sortWith(new FailuresFirstSorter(failureNames));
		}
		return request;
	}

	private boolean isJUnit3SetUpTest(Class<?> clazz, String testName) {
		if (!Test.class.isAssignableFrom(clazz))
			return false;
		try {
			Method testMethod= clazz.getMethod(testName);
			if (testMethod.getAnnotation(org.junit.Test.class) != null)
				return false;

			Method setup= clazz.getMethod(JUnit3TestLoader.SET_UP_TEST_METHOD_NAME, new Class[] { Test.class });
			int modifiers= setup.getModifiers();
			if (setup.getReturnType() == Test.class && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
				return true;
		} catch (SecurityException e1) {
		} catch (NoSuchMethodException e) {
		}
		return false;
	}

	@Override
	public ITestReference[] loadTests(Class[] testClasses, String testName, String[] failureNames, String[] packages, String[][] includeExcludeTags, String uniqueId, RemoteTestRunner listener) {
		return loadTests(testClasses, testName, failureNames, listener);
	}
}