/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.tests;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.junit.Assert;
import org.junit.Test;

public class ImpexRegistrationTest {

	@Test
	public void recognizesImpexWithoutClaimingCsvFiles() {
		final IContentType type = Platform.getContentTypeManager()
				.getContentType("com.hybris.hyeclipse.impex.impex");
		Assert.assertNotNull(type);
		Assert.assertTrue(type.isAssociatedWith("example.impex"));
		Assert.assertFalse(type.isAssociatedWith("example.csv"));
	}

	@Test
	public void previewEditorClassExistsAndIsNotDefault() throws Exception {
		for (final IConfigurationElement element : Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.ui.editors")) {
			if ("com.hybris.hyeclipse.impex.editor".equals(element.getAttribute("id"))) {
				Assert.assertEquals("false", element.getAttribute("default"));
				Assert.assertNotNull(Platform.getBundle("com.hybris.hyeclipse.impex")
						.loadClass(element.getAttribute("class")));
				return;
			}
		}
		Assert.fail("ImpEx preview editor is not registered");
	}

	@Test
	public void grammarBindingAndPresentationClassExist() throws Exception {
		boolean bound = false;
		for (final IConfigurationElement element : Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.tm4e.registry.grammars")) {
			if ("scopeNameContentTypeBinding".equals(element.getName())
					&& "com.hybris.hyeclipse.impex.impex".equals(element.getAttribute("contentTypeId"))) {
				Assert.assertEquals("text.tabular.impex", element.getAttribute("scopeName"));
				bound = true;
			}
		}
		Assert.assertTrue("ImpEx grammar is not bound to its content type", bound);
		for (final IConfigurationElement element : Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.ui.genericeditor.presentationReconcilers")) {
			if ("com.hybris.hyeclipse.impex.impex".equals(element.getAttribute("contentType"))) {
				Assert.assertNotNull(Platform.getBundle("com.hybris.hyeclipse.impex")
						.loadClass(element.getAttribute("class")));
				return;
			}
		}
		Assert.fail("ImpEx presentation reconciler is not registered");
	}
}
