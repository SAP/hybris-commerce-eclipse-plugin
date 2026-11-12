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
package com.hybris.yps.hyeclipse.typesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.hyeclipse.commons.typesystem.TypeSystemRepository;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;
import com.hybris.hyeclipse.commons.typesystem.YType;

public class TypeSystemRepositoryTest {
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void roundTripsTypesAttributesAndSourceLocationsAcrossRestart() throws Exception {
		final File home = temporary.newFolder("platform");
		final File state = temporary.newFolder("state");
		final TypeSystemSnapshot original = snapshot("Dealer");
		final TypeSystemRepository repository = new TypeSystemRepository(state);
		repository.save(home, original);
		Assert.assertSame(original, repository.peek(home));
		final TypeSystemRepository reopened = new TypeSystemRepository(state);
		Assert.assertTrue(reopened.peek(home).getTypes().isEmpty());
		final TypeSystemSnapshot loaded = reopened.load(home).get();
		Assert.assertSame(loaded, reopened.peek(home));
		Assert.assertEquals(Arrays.asList("Dealer"), loaded.getAllTypeNames());
		Assert.assertEquals("example.DealerModel", loaded.getType("Dealer").getJavaClassName());
		Assert.assertEquals("example.BaseModel", loaded.getType("Dealer").getSuperClassName());
		Assert.assertEquals(3, loaded.getType("Dealer").getSourceLine());
		final YAttributeDescriptor attribute = loaded.getAttribute("Dealer", "dealer");
		Assert.assertEquals("java.util.List<java.lang.String>", attribute.getJavaType());
		Assert.assertEquals("example.BaseModel", attribute.getDeclaringClass());
		Assert.assertEquals("/platform/bootstrap/gensrc/BaseModel.java", attribute.getSourcePath());
		Assert.assertEquals(12, attribute.getSourceLine());
	}

	@Test
	public void isolatesWorkspacesPlatformsAndReplacesDeletedTypes() throws Exception {
		final File home = temporary.newFolder("platform");
		final File other = temporary.newFolder("other-platform");
		final File state = temporary.newFolder("state");
		final TypeSystemRepository repository = new TypeSystemRepository(state);
		repository.save(home, snapshot("Dealer"));
		repository.save(other, snapshot("Other"));
		Assert.assertEquals(Arrays.asList("Dealer"), repository.peek(home).getAllTypeNames());
		Assert.assertEquals(Arrays.asList("Other"), repository.peek(other).getAllTypeNames());
		Assert.assertFalse(new TypeSystemRepository(temporary.newFolder("other-state")).load(home).isPresent());
		repository.save(home, TypeSystemSnapshot.empty());
		Assert.assertTrue(new TypeSystemRepository(state).load(home).get().getTypes().isEmpty());
	}

	@Test
	public void corruptLoadAndFailedWritesDoNotReplaceTheLastGoodSnapshot() throws Exception {
		final File home = temporary.newFolder("platform");
		final File state = temporary.newFolder("state");
		final TypeSystemRepository repository = new TypeSystemRepository(state);
		final TypeSystemSnapshot original = snapshot("Dealer");
		repository.save(home, original);
		final File[] files = new File(state, "type-system").listFiles();
		Assert.assertNotNull(files);
		Assert.assertEquals(1, files.length);
		Files.writeString(files[0].toPath(), "broken XML");
		Assert.assertThrows(IOException.class, () -> repository.load(home));
		Assert.assertSame(original, repository.peek(home));
		final TypeSystemRepository blocked = new TypeSystemRepository(temporary.newFile("blocked"));
		Assert.assertThrows(IOException.class, () -> blocked.save(home, original));
		Assert.assertTrue(blocked.peek(home).getTypes().isEmpty());
	}

	private static TypeSystemSnapshot snapshot(final String code) {
		final YAttributeDescriptor attribute = new YAttributeDescriptor("dealer", "java.util.List<java.lang.String>",
				"example.BaseModel", "/platform/bootstrap/gensrc/BaseModel.java", 12);
		return new TypeSystemSnapshot(Arrays.asList(new YType(code, "example.DealerModel", "example.BaseModel",
				"/platform/bootstrap/gensrc/DealerModel.java", 3, Arrays.asList(attribute))));
	}
}
