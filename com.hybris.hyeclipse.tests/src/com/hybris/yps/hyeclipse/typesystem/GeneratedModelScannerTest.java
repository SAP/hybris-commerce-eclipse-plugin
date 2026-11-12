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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.commons.typesystem.GeneratedModelScanner;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YType;

public class GeneratedModelScannerTest {
	@Test
	public void extractsCodesInheritedQualifiersAndGetterTypesWithoutLoadingCommerceClasses() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("/platform/bootstrap/gensrc/ItemModel.java",
				"package de.hybris.platform.core.model; import "
				+ "de.hybris.platform.servicelayer.model.annotations.Accessor; "
				+ "public abstract class ItemModel extends AbstractItemModel { "
				+ "public static final String _TYPECODE = \"Item\"; public static final String PK = \"pk\"; "
				+ "@Accessor(qualifier=PK) public abstract String getPk(); }");
		sources.put("/platform/bootstrap/gensrc/ParentModel.java", source("ParentModel", "ItemModel",
				"public static final String _TYPECODE = \"Parent\"; public static final String CODE = \"code\"; "
				+ "@Accessor(qualifier=CODE) public abstract String getCode(); "
				+ "@Accessor(qualifier=\"ignoredSetter\") public abstract void notAnAccessor();"));
		sources.put("/platform/bootstrap/gensrc/DealerModel.java", source("DealerModel", "ParentModel",
				"public static final String _TYPECODE = (\"Deal\" + \"er\"); "
				+ "public static final String DEALER = \"dealer\"; public static final String NOISE=\"notAnAttribute\"; "
				+ "// @Accessor(qualifier=\"comment\")\n"
				+ "@Accessor(qualifier=DEALER) public abstract void setDealer(Object value); "
				+ "@Accessor(qualifier=DealerModel.DEALER) public abstract String getDealer(); "
				+ "@Accessor(qualifier=CODE) public abstract Integer getCode(); "
				+ "@Accessor(qualifier=\"members\") public abstract java.util.List<ParentModel> getMembers(); "
				+ "@Accessor(qualifier=\"field\") private boolean enabled; "
				+ "public abstract String getUnannotated();"));
		final TypeSystemSnapshot snapshot = scan(sources);
		Assert.assertEquals(Arrays.asList("Dealer", "Item", "Parent"), snapshot.getAllTypeNames());
		final YType dealer = snapshot.getType("Dealer");
		Assert.assertEquals(Arrays.asList("code", "dealer", "field", "members", "pk"), dealer.getAttributeNames());
		Assert.assertEquals("java.lang.String", dealer.getAttribute("dealer").getJavaType());
		Assert.assertEquals("java.lang.Integer", dealer.getAttribute("code").getJavaType());
		Assert.assertEquals("java.util.List<example.model.ParentModel>", dealer.getAttribute("members").getJavaType());
		Assert.assertEquals("de.hybris.platform.core.model.ItemModel", dealer.getAttribute("pk").getDeclaringClass());
		Assert.assertEquals("boolean", dealer.getAttribute("field").getJavaType());
		Assert.assertTrue(dealer.getSourceLine() > 0);
		Assert.assertTrue(dealer.getAttribute("dealer").getSourcePath().startsWith("/platform/bootstrap/gensrc/"));
		Assert.assertTrue(snapshot.getAllAttributeNames("Unknown").isEmpty());
		Assert.assertNull(snapshot.getAttribute("Unknown", "dealer"));
		Assert.assertThrows(UnsupportedOperationException.class, () -> dealer.getAttributeNames().add("mutation"));
	}

	@Test
	public void excludesNonModelsAndClassesWithoutTheirOwnTypeCode() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("plain", source("Plain", "Object", "public static final String _TYPECODE=\"Wrong\";"));
		sources.put("base", source("BaseModel", "ItemModel", "public static final String _TYPECODE=\"Base\";"));
		sources.put("helper", source("HelperModel", "BaseModel", ""));
		Assert.assertEquals(Arrays.asList("Base"), scan(sources).getAllTypeNames());
	}

	@Test
	public void supportsFullyQualifiedAnnotationsAndStaticQualifierImports() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("base", source("BaseModel", "ItemModel",
				"public static final String _TYPECODE=\"Base\"; public static final String CODE=\"code\";"));
		sources.put("model", "package example.model; import static example.model.BaseModel.CODE; "
				+ "public abstract class ChildModel extends example.model.BaseModel { "
				+ "public static final String _TYPECODE=\"Child\"; "
				+ "@de.hybris.platform.servicelayer.model.annotations.Accessor(qualifier=CODE) "
				+ "public abstract long getCode(); }");
		Assert.assertEquals("long", scan(sources).getAttribute("Child", "code").getJavaType());
	}

	@Test
	public void includesNestedModelClassesAndTheirInheritedAttributes() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("model", "package example.model; import de.hybris.platform.core.model.ItemModel; "
				+ "import de.hybris.platform.servicelayer.model.annotations.Accessor; public class Models { "
				+ "public static abstract class BaseModel extends ItemModel { "
				+ "public static final String _TYPECODE=\"Base\"; "
				+ "@Accessor(qualifier=\"base\") public abstract String getBase(); } "
				+ "public static abstract class ChildModel extends BaseModel { "
				+ "public static final String _TYPECODE=\"Child\"; } }");
		final TypeSystemSnapshot snapshot = scan(sources);
		Assert.assertEquals(Arrays.asList("Base", "Child"), snapshot.getAllTypeNames());
		Assert.assertEquals("example.model.Models.ChildModel", snapshot.getType("Child").getJavaClassName());
		Assert.assertEquals(Arrays.asList("base"), snapshot.getAllAttributeNames("Child"));
	}

	@Test
	public void supportsLocalizedAccessorsAndWildcardImports() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("model", "package example.model; import de.hybris.platform.core.model.*; "
				+ "import de.hybris.platform.servicelayer.model.annotations.*; import java.util.*; "
				+ "public abstract class LocalizedModel extends ItemModel { "
				+ "public static final String _TYPECODE=\"Localized\"; "
				+ "@Accessor(qualifier=\"name\") public abstract String getName(Locale locale); "
				+ "@Accessor(qualifier=\"names\") public abstract List<String> getNames(); }");
		final TypeSystemSnapshot snapshot = scan(sources);
		Assert.assertEquals("java.lang.String", snapshot.getAttribute("Localized", "name").getJavaType());
		Assert.assertEquals("java.util.List<java.lang.String>", snapshot.getAttribute("Localized", "names").getJavaType());
	}

	@Test
	public void rejectsInvalidSourcesConstantsAndDuplicateCodes() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("invalid", "public class Broken {");
		Assert.assertThrows(IOException.class, () -> scan(sources));
		sources.clear();
		sources.put("invalid", source("Broken", "ItemModel", "public static final String _TYPECODE=unknown();"));
		Assert.assertThrows(IOException.class, () -> scan(sources));
		sources.clear();
		sources.put("first", source("First", "ItemModel", "public static final String _TYPECODE=\"Duplicate\";"));
		sources.put("second", source("Second", "ItemModel", "public static final String _TYPECODE=\"Duplicate\";"));
		Assert.assertThrows(IOException.class, () -> scan(sources));
	}

	@Test
	public void rejectsInheritanceCyclesAndHonorsCancellation() throws Exception {
		final Map<String, String> sources = new HashMap<>();
		sources.put("first", source("First", "Second", "public static final String _TYPECODE=\"First\";"));
		sources.put("second", source("Second", "First", "public static final String _TYPECODE=\"Second\";"));
		Assert.assertThrows(IOException.class, () -> scan(sources));
		final NullProgressMonitor monitor = new NullProgressMonitor();
		monitor.setCanceled(true);
		Assert.assertThrows(OperationCanceledException.class,
				() -> new GeneratedModelScanner().scanSources(sources, monitor));
	}

	private static TypeSystemSnapshot scan(final Map<String, String> sources) throws IOException {
		return new GeneratedModelScanner().scanSources(sources, new NullProgressMonitor());
	}

	static String source(final String name, final String parent, final String members) {
		return "package example.model; import de.hybris.platform.core.model.ItemModel; "
				+ "import de.hybris.platform.servicelayer.model.annotations.Accessor; "
				+ "public abstract class " + name + " extends " + parent + " { " + members + " }";
	}
}
