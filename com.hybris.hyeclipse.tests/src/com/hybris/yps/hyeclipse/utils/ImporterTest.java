package com.hybris.yps.hyeclipse.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImporterTest {
	
	Importer testObj;
	
	@Before
	public void before() {
		testObj = new Importer();
	}
	
	@Test
	public void test_convertPlatformVersion6503() {
		String pv6503 = "6.5.0.3";
		Double _65d = 6.5d;
		Double zero = 0.0d;
		Double ret = testObj.convertPlatformVersion(pv6503, zero);
		Assert.assertNotNull(ret);
		Assert.assertNotEquals(zero, ret);
		Assert.assertEquals(_65d, ret);
	}
	
	@Test
	public void test_convertPlatformVersion1808() {
		String pv1808 = "18.08";
		Double _18d = 18.08d;
		Double zero = 0.0d;
		Double ret = testObj.convertPlatformVersion(pv1808, zero);
		Assert.assertNotNull(ret);
		Assert.assertNotEquals(zero, ret);
		Assert.assertEquals(_18d, ret);
	}
	
	@Test
	public void test_convertPlatformVersion1905() {
		String pv19 = "19.05";
		Double _19d = 19.05d;
		Double zero = 0.0d;
		Double ret = testObj.convertPlatformVersion(pv19, zero);
		Assert.assertNotNull(ret);
		Assert.assertNotEquals(zero, ret);
		Assert.assertEquals(_19d, ret);
	}

	@Test
	public void test_convertPlatformVersion2005() {
		String pv20 = "20.05";
		Double _20d = 20.05d;
		Double zero = 0.0d;
		Double ret = testObj.convertPlatformVersion(pv20, zero);
		Assert.assertNotNull(ret);
		Assert.assertNotEquals(zero, ret);
		Assert.assertEquals(_20d, ret);
	}
}
