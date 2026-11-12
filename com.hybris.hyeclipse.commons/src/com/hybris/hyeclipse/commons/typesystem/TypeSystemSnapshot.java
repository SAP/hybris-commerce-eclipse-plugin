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
package com.hybris.hyeclipse.commons.typesystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Read-only type index published atomically to all consumers. */
public final class TypeSystemSnapshot {
	private final Map<String, YType> types;

	/** @param types source-derived model types */
	public TypeSystemSnapshot(final Collection<YType> types) {
		final Map<String, YType> byCode = new TreeMap<>();
		for (final YType type : types) {
			if (byCode.put(type.getCode(), type) != null) {
				throw new IllegalArgumentException("Duplicate Commerce type code: " + type.getCode());
			}
		}
		this.types = Collections.unmodifiableMap(byCode);
	}

	/** @return empty snapshot for unavailable or not-yet-indexed projects */
	public static TypeSystemSnapshot empty() {
		return new TypeSystemSnapshot(Collections.emptyList());
	}

	/** @return types sorted by type code */
	public Collection<YType> getTypes() {
		return types.values();
	}

	/** @return sorted type codes */
	public List<String> getAllTypeNames() {
		return Collections.unmodifiableList(new ArrayList<>(types.keySet()));
	}

	/**
	 * @param code Commerce type code
	 * @return type, or null when absent
	 */
	public YType getType(final String code) {
		return types.get(code);
	}

	/**
	 * @param code type code
	 * @return attribute names, or an empty list for unknown types
	 */
	public List<String> getAllAttributeNames(final String code) {
		final YType type = getType(code);
		return type != null ? type.getAttributeNames() : Collections.emptyList();
	}

	/**
	 * @param code type code
	 * @param qualifier attribute name
	 * @return attribute, or null when absent
	 */
	public YAttributeDescriptor getAttribute(final String code, final String qualifier) {
		final YType type = getType(code);
		return type != null ? type.getAttribute(qualifier) : null;
	}
}
