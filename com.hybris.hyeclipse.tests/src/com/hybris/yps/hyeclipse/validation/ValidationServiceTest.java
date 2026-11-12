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
package com.hybris.yps.hyeclipse.validation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.hyeclipse.commons.validation.ItemsSchema;
import com.hybris.hyeclipse.commons.validation.ValidationResult;
import com.hybris.hyeclipse.commons.validation.ValidationResult.State;
import com.hybris.hyeclipse.commons.validation.ValidationService;
import com.hybris.hyeclipse.commons.validation.ValidationXml;

public class ValidationServiceTest {
	private static final String ROOTS = "<itemtype code=\"Item\" abstract=\"true\"/>"
			+ "<itemtype code=\"GenericItem\" extends=\"Item\" abstract=\"true\">"
			+ "<deployment table=\"genericitems\" typecode=\"1\"/></itemtype>";
	private final NullProgressMonitor monitor = new NullProgressMonitor();
	private final ValidationService service = new ValidationService();
	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void adoptsAllNineSourceRules() {
		Assert.assertEquals(9, new HashSet<>(service.getSchemaRuleIds()).size());
	}

	@Test
	public void preservesXPathChecksAndSourceLinesWithoutJaxb() throws Exception {
		final File file = temporary.newFile("custom-items.xml");
		Files.writeString(file.toPath(), "<items>\n<itemtypes>\n<itemtype code=\"wrong\" extends=\"GenericItem\">"
				+ "<attributes><attribute qualifier=\"Name\"><modifiers write=\"false\"/></attribute></attributes>"
				+ "</itemtype></itemtypes></items>");
		final List<ValidationResult> results = service.analyze(file, monitor);
		Assert.assertEquals(State.FAIL, result(results, "wrong", "TypeNameMustStartWithUppercaseLetter").getState());
		Assert.assertEquals(3, result(results, "wrong", "TypeNameMustStartWithUppercaseLetter").getLine());
		Assert.assertEquals(State.FAIL, result(results, "wrong.Name", "ImmutableFieldMustHaveInitialValue").getState());
	}

	@Test
	public void handlesDeploymentHierarchyRatherThanOnlyTheImmediateParent() throws Exception {
		final List<ValidationResult> results = extended(ROOTS
				+ item("Base", "GenericItem", "<deployment table=\"base\" typecode=\"10001\"/>")
				+ item("Middle", "Base", "")
				+ item("Duplicate", "Middle", "<deployment table=\"duplicate\" typecode=\"10002\"/>")
				+ item("NeedsTable", "GenericItem", "")
				+ item("HasTable", "GenericItem", "<deployment table=\"hasTable\" typecode=\"10003\"/>"), "");
		Assert.assertEquals(State.FAIL, result(results, "Duplicate",
				"NoDeploymentTableShouldExistForItemIfNotExtendingGenericItem").getState());
		Assert.assertEquals(State.FAIL, result(results, "NeedsTable",
				"DeploymentTableMustExistForItemExtendingGenericItem").getState());
		Assert.assertEquals(State.PASS, result(results, "HasTable",
				"DeploymentTableMustExistForItemExtendingGenericItem").getState());
	}

	@Test
	public void mergesInheritedIndexesAndComparesQualifiersInsteadOfOnlyCounts() throws Exception {
		final List<ValidationResult> results = extended(ROOTS
				+ item("Base", "GenericItem", attrs("<attribute qualifier=\"code\"><modifiers unique=\"true\"/></attribute>")
						+ index("codeIndex", false, "code"))
				+ item("Child", "Base", "")
				+ item("Mismatch", "GenericItem", attrs("<attribute qualifier=\"code\"><modifiers unique=\"true\"/></attribute>")
						+ index("wrong", false, "wrongQualifier"))
				+ item("NoUnique", "GenericItem", "")
				+ item("BadUniqueIndex", "GenericItem", attrs("<attribute qualifier=\"ordinary\"/>")
						+ index("bad", true, "ordinary")), "");
		Assert.assertEquals(State.PASS, result(results, "Child", "TypeHasAtLeastOneIndexSpecified").getState());
		Assert.assertEquals(State.PASS, result(results, "Child", "NoDatabaseIndexesForUniqueAttributesOfType").getState());
		Assert.assertEquals(State.FAIL, result(results, "Mismatch", "NoDatabaseIndexesForUniqueAttributesOfType").getState());
		Assert.assertEquals(State.FAIL, result(results, "NoUnique", "NoUniqueAttributesDefinedForType").getState());
		Assert.assertEquals(State.FAIL, result(results, "BadUniqueIndex", "UniqueIndexUsingNonUniqueAttributes").getState());
	}

	@Test
	public void evaluatesCatalogVersionAndCatalogKeysIncludingCoveringUniqueIndexes() throws Exception {
		final String properties = "<custom-properties>"
				+ "<property name=\"catalogVersionAttributeQualifier\"><value>\"catalogVersion\"</value></property>"
				+ "<property name=\"uniqueKeyAttributeQualifier\"><value>\"code, catalogVersion\"</value></property>"
				+ "</custom-properties>";
		final String attributes = attrs("<attribute qualifier=\"code\"><modifiers unique=\"true\"/></attribute>"
				+ "<attribute qualifier=\"catalogVersion\"/>");
		final List<ValidationResult> results = extended(ROOTS
				+ item("CatalogMissing", "GenericItem", properties + attributes)
				+ item("CatalogIndexed", "GenericItem", properties + attributes
						+ index("catalogKey", true, "code", "catalogVersion")), "");
		Assert.assertEquals(State.FAIL, result(results, "CatalogMissing",
				"CatalogVersionAttributeShouldBeMarkedUniqueForCatalogAwareTypes").getState());
		Assert.assertEquals(State.FAIL, result(results, "CatalogMissing",
				"UniqueAttributesDontMatchCatalogUniqueAttributeKey").getState());
		Assert.assertEquals(State.PASS, result(results, "CatalogIndexed",
				"UniqueAttributesDontMatchCatalogUniqueAttributeKey").getState());
	}

	@Test
	public void evaluatesBothDirectionsOfCatalogAwareRelationsAndPartOf() throws Exception {
		final String items = ROOTS + item("Catalog", "GenericItem", "<custom-properties>"
				+ "<property name=\"catalogVersionAttributeQualifier\"><value>\"catalogVersion\"</value></property>"
				+ "</custom-properties>") + item("Plain", "GenericItem", "");
		final List<ValidationResult> results = extended(items,
				relation("Forward", "Catalog", "one", "Plain", "many", false)
				+ relation("Reverse", "Plain", "many", "Catalog", "one", false)
				+ relation("Owned", "Catalog", "one", "Plain", "many", true));
		Assert.assertEquals(State.FAIL, result(results, "Forward",
				"One2ManyRelationWhereTheOneSideIsCatalogAwareTypeAndTheManySideIsNot").getState());
		Assert.assertEquals(State.FAIL, result(results, "Reverse",
				"One2ManyRelationWhereTheOneSideIsCatalogAwareTypeAndTheManySideIsNot").getState());
		Assert.assertEquals(State.PASS, result(results, "Owned",
				"One2ManyRelationWhereTheOneSideIsCatalogAwareTypeAndTheManySideIsNot").getState());
	}

	@Test
	public void includesUniqueCatalogAttributesDeclaredByRelationEnds() throws Exception {
		final List<ValidationResult> results = extended(ROOTS
				+ item("CatalogVersion", "GenericItem", "")
				+ item("Product", "GenericItem", "<custom-properties>"
						+ "<property name=\"catalogVersionAttributeQualifier\"><value>\"catalogVersion\"</value></property>"
						+ "</custom-properties>"),
				"<relation code=\"CatalogVersion2Product\"><sourceElement type=\"CatalogVersion\" "
						+ "qualifier=\"catalogVersion\" cardinality=\"one\"><modifiers unique=\"true\"/></sourceElement>"
						+ "<targetElement type=\"Product\" qualifier=\"products\" cardinality=\"many\"/></relation>");
		Assert.assertEquals(State.PASS, result(results, "Product",
				"CatalogVersionAttributeShouldBeMarkedUniqueForCatalogAwareTypes").getState());
		Assert.assertEquals(State.PASS, result(results, "Product", "NoUniqueAttributesDefinedForType").getState());
	}

	@Test
	public void redeclarationAndRemovedIndexesAreHandledAcrossFiles() throws Exception {
		final File base = new File("base-items.xml");
		final File custom = new File("custom-items.xml");
		final Map<File, Document> documents = new LinkedHashMap<>();
		documents.put(base, parse(wrap(ROOTS + item("Base", "GenericItem", index("baseIndex", false, "code")), "")));
		final Document redeclared = parse(wrap("<itemtype code=\"Base\" autocreate=\"false\" generate=\"false\">"
				+ "<indexes><index name=\"baseIndex\" remove=\"true\"/></indexes></itemtype>", ""));
		documents.put(custom, redeclared);
		final ItemsSchema schema = ItemsSchema.build(documents);
		Assert.assertTrue(schema.getItem("Base").getIndexes().isEmpty());
		final List<ValidationResult> results = service.analyze(custom, redeclared, schema, true, monitor);
		Assert.assertEquals(State.PASS, result(results, "Base", "TypeHasAtLeastOneIndexSpecified").getState());
		Assert.assertEquals(State.PASS, result(results, "Base", "NoUniqueAttributesDefinedForType").getState());
	}

	@Test
	public void missingParentsAndUnknownRedeclarationsAreSkippedNotPassed() throws Exception {
		final List<ValidationResult> results = extended(ROOTS + item("Orphan", "Missing", "")
				+ "<itemtype code=\"Unknown\" autocreate=\"false\" generate=\"false\"/>", "");
		Assert.assertEquals(State.SKIPPED, result(results, "Orphan", "NoUniqueAttributesDefinedForType").getState());
		Assert.assertEquals(State.SKIPPED, result(results, "Unknown", "NoUniqueAttributesDefinedForType").getState());
	}

	@Test
	public void rejectsCyclesExternalEntitiesAndHonorsCancellation() throws Exception {
		final Map<File, Document> documents = new LinkedHashMap<>();
		documents.put(new File("cycle.xml"), parse(wrap(item("A", "B", "") + item("B", "A", ""), "")));
		Assert.assertThrows(IOException.class, () -> ItemsSchema.build(documents));
		Assert.assertThrows(IOException.class, () -> parse("<!DOCTYPE items [<!ENTITY x SYSTEM 'file:///etc/passwd'>]>"
				+ "<items>&x;</items>"));
		monitor.setCanceled(true);
		Assert.assertThrows(OperationCanceledException.class, () -> parse("<items/>"));
	}

	@Test
	public void scansImportedFilesAndDoesNotHideInputErrors() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File core = new File(platform, "ext/core");
		Files.createDirectories(core.toPath().resolve("resources"));
		Files.writeString(core.toPath().resolve("resources/core-items.xml"), wrap(ROOTS
				+ item("NeedsTable", "GenericItem", ""), ""));
		final SystemConfig config = SystemConfig.parse(Arrays.asList("[echo] Platform home: " + platform,
				"[echo] hybris Platform version: 2211", "[echo] core 2211 [pci]"));
		final List<ValidationResult> results = service.analyze(config,
				Arrays.asList(core.toPath().resolve("resources/core-items.xml").toFile()), monitor);
		Assert.assertEquals(State.FAIL, result(results, "NeedsTable",
				"DeploymentTableMustExistForItemExtendingGenericItem").getState());
		final File missing = new File(temporary.getRoot(), "missing-items.xml");
		Assert.assertEquals(State.ERROR, service.analyze(missing, monitor).get(0).getState());
		Assert.assertEquals(State.ERROR, service.analyze(missing, parse("<notItems/>"), null, false, monitor)
				.get(0).getState());
	}

	private List<ValidationResult> extended(final String items, final String relations) throws IOException {
		final File file = new File("custom-items.xml");
		final Document document = parse(wrap(items, relations));
		final Map<File, Document> sources = new LinkedHashMap<>();
		sources.put(file, document);
		return service.analyze(file, document, ItemsSchema.build(sources), true, monitor);
	}
	private Document parse(final String xml) throws IOException {
		return ValidationXml.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), monitor);
	}
	private static String wrap(final String items, final String relations) {
		return "<items><itemtypes>" + items + "</itemtypes><relations>" + relations + "</relations></items>";
	}
	private static String item(final String code, final String parent, final String contents) {
		return "<itemtype code=\"" + code + "\" extends=\"" + parent + "\">" + contents + "</itemtype>";
	}
	private static String attrs(final String contents) { return "<attributes>" + contents + "</attributes>"; }
	private static String index(final String name, final boolean unique, final String... qualifiers) {
		final StringBuilder xml = new StringBuilder("<indexes><index name=\"").append(name)
				.append("\" unique=\"").append(unique).append("\">");
		for (final String qualifier : qualifiers) {
			xml.append("<key attribute=\"").append(qualifier).append("\"/>");
		}
		return xml.append("</index></indexes>").toString();
	}
	private static String relation(final String code, final String source, final String sourceCardinality,
			final String target, final String targetCardinality, final boolean partOf) {
		return "<relation code=\"" + code + "\"><sourceElement type=\"" + source + "\" cardinality=\""
				+ sourceCardinality + "\" qualifier=\"source\"><modifiers partof=\"" + partOf + "\"/></sourceElement>"
				+ "<targetElement type=\"" + target + "\" cardinality=\"" + targetCardinality
				+ "\" qualifier=\"target\"><modifiers partof=\"" + partOf + "\"/></targetElement></relation>";
	}
	private static ValidationResult result(final List<ValidationResult> results, final String element, final String id) {
		for (final ValidationResult result : results) {
			if (element.equals(result.getElement()) && id.equals(result.getRuleId())) {
				return result;
			}
		}
		throw new AssertionError("Missing finding " + id + " for " + element);
	}
}
