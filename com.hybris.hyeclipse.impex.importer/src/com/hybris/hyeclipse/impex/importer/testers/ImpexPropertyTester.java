package com.hybris.hyeclipse.impex.importer.testers;

import com.hybris.hyeclipse.testers.AbstractFilePropertyTester;

/**
 * Property tester checking whether the file is impex.
 */
public class ImpexPropertyTester extends AbstractFilePropertyTester {

	public static final String PROPERTY_IS_IMPEX = "isImpex";
	private static final String IMPEX_EXTENSION = "impex";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		return property.equals(PROPERTY_IS_IMPEX) && testSelectedFileByExtension(receiver, IMPEX_EXTENSION);
	}
}
