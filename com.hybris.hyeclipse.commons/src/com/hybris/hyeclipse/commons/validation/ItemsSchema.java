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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/** Source-built schema metadata used by TSV; Java model accessors cannot provide these modifiers. */
public final class ItemsSchema {
	private final Map<String, Item> items = new LinkedHashMap<>();
	private final Map<String, Relation> relations = new LinkedHashMap<>();

	/**
	 * @param documents items XML in extension dependency order
	 * @return merged schema including inherited attributes, indexes and custom properties
	 * @throws IOException if inheritance is cyclic or a declaration is inconsistent
	 */
	public static ItemsSchema build(final Map<File, Document> documents) throws IOException {
		final ItemsSchema schema = new ItemsSchema();
		for (final Map.Entry<File, Document> entry : documents.entrySet()) {
			if (!"items".equals(entry.getValue().getDocumentElement().getTagName())) {
				throw new IOException("Expected items XML in " + entry.getKey());
			}
			final NodeList types = entry.getValue().getElementsByTagName("itemtype");
			for (int i = 0; i < types.getLength(); i++) {
				final Element type = (Element) types.item(i);
				final String code = type.getAttribute("code");
				if (code.isBlank()) {
					throw new IOException("An itemtype has no code in " + entry.getKey());
				}
				Item item = schema.items.get(code);
				if (item == null) {
					item = new Item(code, entry.getKey());
					item.undeclared = !"Item".equals(code) && !"GenericItem".equals(code)
							&& "false".equals(type.getAttribute("autocreate"))
							&& "false".equals(type.getAttribute("generate"));
					schema.items.put(code, item);
				}
				if (type.hasAttribute("extends")) {
					if (type.getAttribute("extends").isBlank() && !"Item".equals(code)) {
						throw new IOException("An itemtype has an empty superclass in " + entry.getKey());
					}
					item.parent = type.getAttribute("extends");
				}
				if (type.hasAttribute("abstract")) {
					item.abstractType = bool(type, "abstract");
				}
				final Element deployment = ValidationXml.child(type, "deployment");
				if (deployment != null) {
					item.deployment = deployment.getAttribute("table");
				}
				final Element properties = ValidationXml.child(type, "custom-properties");
				if (properties != null) {
					for (final Element property : ValidationXml.children(properties, "property")) {
						final Element value = ValidationXml.child(property, "value");
						if (value != null) {
							item.properties.put(property.getAttribute("name"), unquote(value.getTextContent().trim()));
						}
					}
				}
				final Element attributes = ValidationXml.child(type, "attributes");
				if (attributes != null) {
					for (final Element attribute : ValidationXml.children(attributes, "attribute")) {
						final String qualifier = attribute.getAttribute("qualifier");
						if (qualifier.isBlank()) {
							throw new IOException("An attribute has no qualifier in " + entry.getKey());
						}
						final Element modifiers = ValidationXml.child(attribute, "modifiers");
						final Map<String, String> flags = item.attributes.computeIfAbsent(qualifier,
								key -> new HashMap<>());
						if (modifiers != null) {
							final NamedNodeMap values = modifiers.getAttributes();
							for (int j = 0; j < values.getLength(); j++) {
								flags.put(values.item(j).getNodeName(), values.item(j).getNodeValue());
							}
						}
					}
				}
				final Element indexes = ValidationXml.child(type, "indexes");
				if (indexes != null) {
					for (final Element index : ValidationXml.children(indexes, "index")) {
						final List<String> keys = new ArrayList<>();
						for (final Element key : ValidationXml.children(index, "key")) {
							keys.add(key.getAttribute("attribute"));
						}
						item.indexes.put(index.getAttribute("name"), new Index(keys, bool(index, "unique"),
								bool(index, "remove")));
					}
				}
			}
			final NodeList relations = entry.getValue().getElementsByTagName("relation");
			for (int i = 0; i < relations.getLength(); i++) {
				final Element relation = (Element) relations.item(i);
				final Element source = ValidationXml.child(relation, "sourceElement");
				final Element target = ValidationXml.child(relation, "targetElement");
				if (source == null || target == null) {
					throw new IOException("Incomplete relation in " + entry.getKey());
				}
				if (relation.getAttribute("code").isBlank() || source.getAttribute("type").isBlank()
						|| target.getAttribute("type").isBlank()) {
					throw new IOException("A relation has missing type information in " + entry.getKey());
				}
				schema.relations.put(relation.getAttribute("code"), new Relation(new End(source), new End(target)));
			}
		}
		// Each relation end declares an attribute on the item at the opposite end.
		for (final Relation relation : schema.relations.values()) {
			schema.addRelationAttribute(relation.source, relation.target.type);
			schema.addRelationAttribute(relation.target, relation.source.type);
		}
		for (final Item item : schema.items.values()) {
			schema.merge(item, new HashSet<>());
		}
		return schema;
	}

	private void addRelationAttribute(final End end, final String ownerCode) {
		final Item owner = items.get(ownerCode);
		if (owner != null && !end.qualifier.isEmpty()) {
			final Map<String, String> modifiers = new HashMap<>(end.modifiers);
			final Map<String, String> declared = owner.attributes.get(end.qualifier);
			if (declared != null) {
				modifiers.putAll(declared);
			}
			owner.attributes.put(end.qualifier, modifiers);
		}
	}

	private void merge(final Item item, final Set<String> visiting) throws IOException {
		if (item.merged) {
			return;
		}
		if (!visiting.add(item.code)) {
			throw new IOException("Cyclic items XML inheritance: " + item.code);
		}
		final Item parent = items.get(item.parent);
		item.complete = !item.undeclared && (item.parent.isEmpty() || parent != null);
		if (parent != null) {
			merge(parent, visiting);
			item.complete = !item.undeclared && parent.complete;
			final Map<String, Map<String, String>> effective = new LinkedHashMap<>();
			for (final Map.Entry<String, Map<String, String>> inherited : parent.attributes.entrySet()) {
				effective.put(inherited.getKey(), new HashMap<>(inherited.getValue()));
			}
			for (final Map.Entry<String, Map<String, String>> own : item.attributes.entrySet()) {
				effective.computeIfAbsent(own.getKey(), key -> new HashMap<>()).putAll(own.getValue());
			}
			item.attributes = effective;
			final Map<String, Index> effectiveIndexes = new LinkedHashMap<>();
			// The source reference intentionally excludes GenericItem's indexes from these checks.
			if (!"GenericItem".equals(parent.code)) {
				effectiveIndexes.putAll(parent.indexes);
			}
			effectiveIndexes.putAll(item.indexes);
			item.indexes = effectiveIndexes;
			final Map<String, String> properties = new HashMap<>(parent.properties);
			properties.putAll(item.properties);
			item.properties = properties;
		}
		item.indexes.values().removeIf(index -> index.removed);
		item.merged = true;
		visiting.remove(item.code);
	}

	/** @param code item type code @return schema type or null */
	public Item getItem(final String code) { return items.get(code); }
	/** @param code relation code @return schema relation or null */
	public Relation getRelation(final String code) { return relations.get(code); }

	boolean extendsAny(final Item item, final Set<String> names) {
		Item current = item;
		while (current != null) {
			if (names.contains(current.code)) {
				return true;
			}
			current = items.get(current.parent);
		}
		return false;
	}

	Item deploymentAncestor(final Item item) {
		Item parent = items.get(item.parent);
		while (parent != null && parent.deployment.isEmpty()) {
			parent = items.get(parent.parent);
		}
		return parent;
	}

	static boolean bool(final Element element, final String attribute) {
		return Boolean.parseBoolean(element.getAttribute(attribute));
	}

	static String unquote(final String text) {
		return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")
				? text.substring(1, text.length() - 1) : text;
	}

	/** Read-only effective item metadata. */
	public static final class Item {
		private final String code;
		private final File firstFile;
		private String parent = "";
		private String deployment = "";
		private boolean abstractType;
		private boolean undeclared;
		private boolean complete;
		private boolean merged;
		private Map<String, String> properties = new HashMap<>();
		private Map<String, Map<String, String>> attributes = new LinkedHashMap<>();
		private Map<String, Index> indexes = new LinkedHashMap<>();

		private Item(final String code, final File firstFile) {
			this.code = code;
			this.firstFile = firstFile;
			parent = "Item".equals(code) ? "" : "GenericItem".equals(code) ? "Item" : "GenericItem";
		}
		/** @return type code */
		public String getCode() { return code; }
		/** @return declared superclass */
		public String getParent() { return parent; }
		/** @return declared deployment table */
		public String getDeployment() { return deployment; }
		/** @return abstract modifier */
		public boolean isAbstract() { return abstractType; }
		/** @return whether all ancestors were loaded */
		public boolean isComplete() { return complete; }
		/** @return file declaring the type first */
		public File getFirstFile() { return firstFile; }
		/** @param name custom-property name @return normalized value or null */
		public String getProperty(final String name) { return properties.get(name); }
		/** @return effective unique qualifiers */
		public Set<String> getUniqueAttributes() {
			final Set<String> result = new HashSet<>();
			for (final Map.Entry<String, Map<String, String>> entry : attributes.entrySet()) {
				if (Boolean.parseBoolean(entry.getValue().get("unique"))) {
					result.add(entry.getKey());
				}
			}
			return Collections.unmodifiableSet(result);
		}
		/** @param qualifier attribute name @return whether an attribute is present */
		public boolean hasAttribute(final String qualifier) { return attributes.containsKey(qualifier); }
		/** @return effective index definitions */
		public List<Index> getIndexes() {
			return "GenericItem".equals(code) ? Collections.emptyList()
					: Collections.unmodifiableList(new ArrayList<>(indexes.values()));
		}
	}

	/** Effective index metadata. */
	public static final class Index {
		private final Set<String> attributes;
		private final boolean unique;
		private final boolean removed;
		private Index(final List<String> attributes, final boolean unique, final boolean removed) {
			this.attributes = Collections.unmodifiableSet(new HashSet<>(attributes));
			this.unique = unique;
			this.removed = removed;
		}
		/** @return indexed qualifiers */
		public Set<String> getAttributes() { return attributes; }
		/** @return index unique modifier */
		public boolean isUnique() { return unique; }
	}

	/** Relation endpoints used by catalog-aware relation checks. */
	public static final class Relation {
		private final End source;
		private final End target;
		private Relation(final End source, final End target) { this.source = source; this.target = target; }
		/** @return source endpoint */
		public End getSource() { return source; }
		/** @return target endpoint */
		public End getTarget() { return target; }
	}

	/** Immutable relation endpoint. */
	public static final class End {
		private final String type;
		private final String cardinality;
		private final String qualifier;
		private final Map<String, String> modifiers = new HashMap<>();
		private final boolean partOf;
		private End(final Element element) {
			type = element.getAttribute("type");
			cardinality = element.getAttribute("cardinality");
			qualifier = element.getAttribute("qualifier");
			final Element flags = ValidationXml.child(element, "modifiers");
			if (flags != null) {
				final NamedNodeMap attributes = flags.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					modifiers.put(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
				}
			}
			partOf = Boolean.parseBoolean(modifiers.get("partof"));
		}
		/** @return endpoint type */
		public String getType() { return type; }
		/** @return one or many */
		public String getCardinality() { return cardinality; }
		/** @return endpoint partof modifier */
		public boolean isPartOf() { return partOf; }
	}
}
