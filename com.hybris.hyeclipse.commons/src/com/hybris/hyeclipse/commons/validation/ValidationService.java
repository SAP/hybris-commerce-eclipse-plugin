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
package com.hybris.hyeclipse.commons.validation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import com.hybris.hyeclipse.commons.config.ExtensionConfig;
import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.hyeclipse.commons.validation.ItemsSchema.End;
import com.hybris.hyeclipse.commons.validation.ItemsSchema.Index;
import com.hybris.hyeclipse.commons.validation.ItemsSchema.Item;
import com.hybris.hyeclipse.commons.validation.ItemsSchema.Relation;
import com.hybris.hyeclipse.commons.validation.ValidationResult.State;

/** Source-built XPath and toolbox 3.0.1 schema checks; no Spring, Groovy, JAXB or SAP runtime. */
public final class ValidationService {
	private static final Set<String> INDEX_EXEMPT_TYPES = new HashSet<>(Arrays.asList(
			"BaseStore", "SyncItemJob", "CMSItem", "BaseSite", "Consignment"));
	private static final String CATALOG_VERSION = "catalogVersionAttributeQualifier";
	private static final String CATALOG_KEYS = "uniqueKeyAttributeQualifier";
	private static final Pattern QUALIFIER = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
	private static final String SINGLE_DEPLOYMENT = "NoDeploymentTableShouldExistForItemIfNotExtendingGenericItem";
	private static final String REQUIRED_DEPLOYMENT = "DeploymentTableMustExistForItemExtendingGenericItem";
	private static final String REQUIRED_INDEX = "TypeHasAtLeastOneIndexSpecified";
	private static final String UNIQUE_INDEX = "NoDatabaseIndexesForUniqueAttributesOfType";
	private static final String CATALOG_VERSION_UNIQUE = "CatalogVersionAttributeShouldBeMarkedUniqueForCatalogAwareTypes";
	private static final String CATALOG_KEYS_UNIQUE = "UniqueAttributesDontMatchCatalogUniqueAttributeKey";
	private static final String CATALOG_RELATION = "One2ManyRelationWhereTheOneSideIsCatalogAwareTypeAndTheManySideIsNot";
	private static final String REQUIRED_UNIQUE = "NoUniqueAttributesDefinedForType";
	private static final String NON_UNIQUE_INDEX = "UniqueIndexUsingNonUniqueAttributes";
	private static final List<String> SCHEMA_RULES = Arrays.asList(SINGLE_DEPLOYMENT, REQUIRED_DEPLOYMENT,
			REQUIRED_INDEX, UNIQUE_INDEX, CATALOG_VERSION_UNIQUE, CATALOG_KEYS_UNIQUE, CATALOG_RELATION,
			REQUIRED_UNIQUE, NON_UNIQUE_INDEX);
	private final Properties messages = new Properties();

	/** Loads the source-built rule descriptions. */
	public ValidationService() {
		try (final InputStream input = ValidationService.class.getResourceAsStream("validation.properties")) {
			if (input == null) {
				throw new IOException("Missing validation descriptions");
			}
			messages.load(input);
		} catch (IOException e) { throw new IllegalStateException("Could not load validation descriptions", e); }
	}

	/** @return adopted toolbox type-system rule identifiers */
	public List<String> getSchemaRuleIds() {
		return java.util.Collections.unmodifiableList(SCHEMA_RULES);
	}

	/**
	 * @param file items XML to validate
	 * @param monitor cancellation monitor
	 * @return standard XPath findings including input errors
	 */
	public List<ValidationResult> analyze(final File file, final IProgressMonitor monitor) {
		try (final InputStream input = Files.newInputStream(file.toPath())) {
			return analyze(file, ValidationXml.parse(input, monitor), null, false, monitor);
		} catch (IOException e) { return Arrays.asList(error(file, e)); }
	}

	/**
	 * Builds cross-extension XML metadata in SystemConfig order before checking the selected extensions.
	 * @param config imported platform configuration
	 * @param selected selected items XML files
	 * @param monitor cancellation monitor
	 * @return XPath and all nine schema-rule findings
	 */
	public List<ValidationResult> analyze(final SystemConfig config, final Collection<File> selected,
			final IProgressMonitor monitor) {
		final List<ValidationResult> results = new ArrayList<>();
		final Map<File, Document> documents = new LinkedHashMap<>();
		boolean complete = true;
		for (final ExtensionConfig extension : config.getExtensionsInOrder()) {
			ValidationXml.checkCanceled(monitor);
			final File file = itemsFile(extension);
			if (file == null) {
				if (extension.hasItemsXml()) {
					complete = false;
					final File missing = extension.getExtensionDirectory() != null
							? new File(extension.getExtensionDirectory(), "resources/" + extension.getName() + "-items.xml")
							: config.getPlatformHome();
					results.add(error(missing, new IOException("Missing items XML for " + extension.getName())));
				}
				continue;
			}
			try (final InputStream input = Files.newInputStream(file.toPath())) {
				documents.put(file.getCanonicalFile(), ValidationXml.parse(input, monitor));
			} catch (IOException e) { complete = false; results.add(error(file, e)); }
		}
		ItemsSchema schema = null;
		if (complete) {
			try { schema = ItemsSchema.build(documents); }
			catch (IOException e) { results.add(error(config.getPlatformHome(), e)); }
		}
		for (final File file : selected) {
			ValidationXml.checkCanceled(monitor);
			try {
				Document document = documents.get(file.getCanonicalFile());
				if (document == null) {
					try (final InputStream input = Files.newInputStream(file.toPath())) {
						document = ValidationXml.parse(input, monitor);
					}
				}
				results.addAll(analyze(file.getCanonicalFile(), document, schema, true, monitor));
			} catch (IOException e) { results.add(error(file, e)); }
		}
		return results;
	}

	/**
	 * @param file original source
	 * @param document parsed source
	 * @param schema cross-extension metadata, or null when unavailable
	 * @param extended whether to include schema checks
	 * @param monitor cancellation monitor
	 * @return source-located findings; unavailable metadata is explicitly SKIPPED
	 */
	public List<ValidationResult> analyze(final File file, final Document document, final ItemsSchema schema,
			final boolean extended, final IProgressMonitor monitor) {
		if (!"items".equals(document.getDocumentElement().getTagName())) {
			return Arrays.asList(error(file, new IOException("Expected an items XML document")));
		}
		final List<ValidationResult> results = xpath(file, document, extended, monitor);
		if (!extended) {
			return results;
		}
		for (final String id : SCHEMA_RULES) {
			ValidationXml.checkCanceled(monitor);
			final NodeList nodes = document.getElementsByTagName(CATALOG_RELATION.equals(id) ? "relation" : "itemtype");
			for (int i = 0; i < nodes.getLength(); i++) {
				final Element declaration = (Element) nodes.item(i);
				final String code = declaration.getAttribute("code");
				final State state = schema == null ? State.SKIPPED
						: evaluate(id, code, file, declaration, schema);
				final String message = messages.getProperty(id, id)
						+ (state == State.SKIPPED ? " " + messages.getProperty("skipped") : "");
				results.add(new ValidationResult(file, ValidationXml.line(declaration), id,
						messages.getProperty(id + ".priority", "M"), state, code, message));
			}
		}
		return results;
	}

	private State evaluate(final String id, final String code, final File file, final Element declaration,
			final ItemsSchema schema) {
		if (CATALOG_RELATION.equals(id)) {
			return relation(schema.getRelation(code), schema);
		}
		final Item item = schema.getItem(code);
		if (item == null || !item.isComplete()) {
			return State.SKIPPED;
		}
		final boolean redeclared = !item.getFirstFile().equals(file)
				|| ("false".equals(declaration.getAttribute("autocreate"))
						&& "false".equals(declaration.getAttribute("generate")));
		final Set<String> unique = item.getUniqueAttributes();
		final List<Index> indexes = item.getIndexes();
		if (SINGLE_DEPLOYMENT.equals(id) || REQUIRED_DEPLOYMENT.equals(id)) {
			if (item.isAbstract() || redeclared) {
				return State.PASS;
			}
			final Item parent = schema.deploymentAncestor(item);
			if (parent == null && item.getDeployment().isEmpty()) {
				return State.SKIPPED;
			}
			if (SINGLE_DEPLOYMENT.equals(id)) {
				return state(item.getDeployment().isEmpty() || parent == null || "GenericItem".equals(parent.getCode()));
			}
			return state(!item.getDeployment().isEmpty() || parent == null
					|| !"genericitems".equalsIgnoreCase(parent.getDeployment()));
		}
		if (REQUIRED_INDEX.equals(id)) {
			return state(!indexes.isEmpty() || redeclared || schema.extendsAny(item, INDEX_EXEMPT_TYPES));
		}
		if (UNIQUE_INDEX.equals(id)) {
			return state(unique.isEmpty() || schema.extendsAny(item, INDEX_EXEMPT_TYPES)
					|| indexes.stream().anyMatch(index -> index.getAttributes().equals(unique)));
		}
		if (CATALOG_VERSION_UNIQUE.equals(id)) {
			final String qualifier = item.getProperty(CATALOG_VERSION);
			if (qualifier != null && !QUALIFIER.matcher(qualifier).matches()) {
				return State.SKIPPED;
			}
			return state(qualifier == null || unique.contains(qualifier));
		}
		if (CATALOG_KEYS_UNIQUE.equals(id)) {
			final String value = item.getProperty(CATALOG_KEYS);
			if (value == null) {
				return State.PASS;
			}
			final Set<String> keys = new HashSet<>();
			for (final String key : value.split(",")) {
				if (!QUALIFIER.matcher(key.trim()).matches()) {
					return State.SKIPPED;
				}
				keys.add(key.trim());
			}
			return state(unique.containsAll(keys) || indexes.stream()
					.anyMatch(index -> index.isUnique() && index.getAttributes().containsAll(keys)));
		}
		if (REQUIRED_UNIQUE.equals(id)) {
			return state(!unique.isEmpty() || redeclared);
		}
		if (NON_UNIQUE_INDEX.equals(id)) {
			return state(indexes.stream().filter(Index::isUnique).allMatch(index -> unique.containsAll(index.getAttributes())));
		}
		throw new IllegalArgumentException("Unsupported schema rule: " + id);
	}

	private static State relation(final Relation relation, final ItemsSchema schema) {
		if (relation == null) {
			return State.SKIPPED;
		}
		final End source = relation.getSource();
		final End target = relation.getTarget();
		if ((!"one".equals(source.getCardinality()) && !"many".equals(source.getCardinality()))
				|| (!"one".equals(target.getCardinality()) && !"many".equals(target.getCardinality()))) {
			return State.SKIPPED;
		}
		final Item sourceType = schema.getItem(source.getType());
		final Item targetType = schema.getItem(target.getType());
		if (sourceType == null || targetType == null || !sourceType.isComplete() || !targetType.isComplete()) {
			return State.SKIPPED;
		}
		final boolean sourceCatalog = sourceType.getProperty(CATALOG_VERSION) != null;
		final boolean targetCatalog = targetType.getProperty(CATALOG_VERSION) != null;
		if ((sourceCatalog && !QUALIFIER.matcher(sourceType.getProperty(CATALOG_VERSION)).matches())
				|| (targetCatalog && !QUALIFIER.matcher(targetType.getProperty(CATALOG_VERSION)).matches())) {
			return State.SKIPPED;
		}
		if ("one".equals(source.getCardinality()) && "many".equals(target.getCardinality())) {
			return state(!sourceCatalog || targetCatalog || target.isPartOf());
		}
		if ("many".equals(source.getCardinality()) && "one".equals(target.getCardinality())) {
			return state(!targetCatalog || sourceCatalog || source.isPartOf());
		}
		return State.PASS;
	}

	private static State state(final boolean passed) { return passed ? State.PASS : State.FAIL; }

	private List<ValidationResult> xpath(final File file, final Document source, final boolean extended,
			final IProgressMonitor monitor) {
		final List<ValidationResult> results = new ArrayList<>();
		try (final InputStream input = ValidationService.class.getResourceAsStream("xpath-rules.xml")) {
			if (input == null) {
				throw new IOException("Missing XPath rules");
			}
			final Document rules = ValidationXml.parse(input, monitor);
			final XPath xpath = XPathFactory.newInstance().newXPath();
			for (final Element rule : ValidationXml.children(rules.getDocumentElement(), "rule")) {
				ValidationXml.checkCanceled(monitor);
				final String id = rule.getAttribute("id");
				if (extended && (id.equals(SINGLE_DEPLOYMENT) || id.equals(REQUIRED_DEPLOYMENT))) {
					continue;
				}
				if (!extended && "UseOfUnoptimizedAttributesIsNotRecommended".equals(id)) {
					continue;
				}
				final XPathExpression selection = xpath.compile(rule.getAttribute("selectionQuery"));
				final XPathExpression test = xpath.compile(rule.getAttribute("testQuery"));
				final XPathExpression name = xpath.compile(rule.getAttribute("nameQuery"));
				final NodeList nodes = (NodeList) selection.evaluate(source, XPathConstants.NODESET);
				for (int i = 0; i < nodes.getLength(); i++) {
					final Element node = (Element) nodes.item(i);
					final boolean pass = (Boolean) test.evaluate(node, XPathConstants.BOOLEAN);
					final NodeList names = (NodeList) name.evaluate(node, XPathConstants.NODESET);
					final List<String> parts = new ArrayList<>();
					for (int j = 0; j < names.getLength(); j++) {
						parts.add(names.item(j).getNodeValue());
					}
					results.add(new ValidationResult(file, ValidationXml.line(node), id, rule.getAttribute("priority"),
							state(pass), String.join(".", parts), rule.getAttribute("description")));
				}
			}
		} catch (IOException | XPathExpressionException e) { results.add(error(file, e)); }
		return results;
	}

	/**
	 * @param extension imported extension
	 * @return its items XML file, or null
	 */
	public static File itemsFile(final ExtensionConfig extension) {
		if (extension.getExtensionDirectory() == null) {
			return null;
		}
		for (final String name : Arrays.asList(extension.getName() + "-items.xml",
				extension.getName() + ".items.xml", "items.xml")) {
			final File file = new File(extension.getExtensionDirectory(), "resources/" + name);
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}

	/**
	 * @param directory directory to scan
	 * @param monitor cancellation monitor
	 * @return sorted items XML files
	 * @throws IOException if file discovery fails
	 */
	public static List<File> findItemsFiles(final File directory, final IProgressMonitor monitor) throws IOException {
		try (final Stream<java.nio.file.Path> paths = Files.walk(directory.toPath())) {
			return paths.peek(path -> ValidationXml.checkCanceled(monitor)).filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().endsWith("items.xml"))
					.sorted().map(java.nio.file.Path::toFile).collect(Collectors.toList());
		}
	}

	private ValidationResult error(final File file, final Exception failure) {
		int line = 1;
		Throwable cause = failure;
		while (cause != null) {
			if (cause instanceof SAXParseException) {
				line = ((SAXParseException) cause).getLineNumber();
			}
			cause = cause.getCause();
		}
		return new ValidationResult(file, line, "XML", "H", State.ERROR, "",
				messages.getProperty("xml_error") + " " + failure.getMessage());
	}
}
