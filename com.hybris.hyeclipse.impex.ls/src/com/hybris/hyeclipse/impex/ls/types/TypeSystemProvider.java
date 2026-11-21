/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.ls.types;

import org.eclipse.lsp4j.Location;

/** Host adapter boundary: the language server does not access the workspace or filesystem directly. */
@FunctionalInterface
public interface TypeSystemProvider {

	/**
	 * @param documentUri open document identity
	 * @return immutable metadata belonging to this document, or an empty catalog;
	 *         retain snapshot identity until metadata changes so in-flight results can be checked
	 */
	TypeCatalog getCatalog(String documentUri);

	/**
	 * @param documentUri requesting document identity
	 * @param sourcePath source identity from the catalog
	 * @param sourceLine one-based declaration line
	 * @return accessible source location, or null when unavailable/outside the document's platform
	 */
	default Location resolveLocation(final String documentUri, final String sourcePath, final int sourceLine) {
		return null;
	}
}
