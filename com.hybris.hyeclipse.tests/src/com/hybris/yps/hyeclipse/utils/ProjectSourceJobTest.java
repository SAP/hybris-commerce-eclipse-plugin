/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.utils;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Assert;
import org.junit.Test;

public class ProjectSourceJobTest {

	@Test
	@SuppressWarnings("unchecked")
	public void matchesOnlyHybrisLibraries() throws Exception {
		final Method method = ProjectSourceJob.class.getDeclaredMethod("getHybrisLibs");
		method.setAccessible(true);
		final Predicate<IClasspathEntry> predicate =
				(Predicate<IClasspathEntry>) method.invoke(new ProjectSourceJob());
		Assert.assertTrue(predicate.test(JavaCore.newLibraryEntry(new Path("/lib/coreserver.jar"), null, null)));
		Assert.assertTrue(predicate.test(JavaCore.newLibraryEntry(new Path("/lib/ybootstrap.jar"), null, null)));
		Assert.assertFalse(predicate.test(JavaCore.newLibraryEntry(new Path("/lib/other.jar"), null, null)));
		Assert.assertFalse(predicate.test(JavaCore.newSourceEntry(new Path("/project/server.jar"))));
	}
}
