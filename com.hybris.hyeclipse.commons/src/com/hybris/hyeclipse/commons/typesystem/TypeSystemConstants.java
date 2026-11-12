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

/** Source conventions used by Commerce's generated model classes. */
public final class TypeSystemConstants {
	public static final String GENERATED_SOURCE_DIRECTORY = "bootstrap/gensrc";
	public static final String ITEM_MODEL_CLASS = "de.hybris.platform.core.model.ItemModel";
	public static final String ACCESSOR_CLASS = "de.hybris.platform.servicelayer.model.annotations.Accessor";
	public static final String TYPE_CODE_FIELD = "_TYPECODE";
	public static final String QUALIFIER_MEMBER = "qualifier";

	private TypeSystemConstants() {
		/* intentionally empty */
	}
}
