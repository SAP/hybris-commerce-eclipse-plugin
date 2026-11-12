/*******************************************************************************
 * Copyright 2020 SAP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.yps.hyeclipse.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.hybris.hyeclipse.commons.config.ExtensionConfig;
import com.hybris.hyeclipse.commons.config.SystemConfig;

public class SystemConfigTest {

	private static final List<String> SAMPLE_LOG = Arrays.asList(
			"Buildfile: /Users/XXX/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/platform/build.xml",
			"[ysetplatformproperties] ---------------------------------------------------------------",
			"[ysetplatformproperties] Warning: you're using at least one deprecated extension!",
			"[ysetplatformproperties] Please note that they may not be available in future releases.",
			"[ysetplatformproperties] ",
			"[ysetplatformproperties] Deprecated extensions: [acceleratorstorefrontcommons, acceleratorwebservicesaddon, addonsupport, assistedservicestorefront, captchaaddon, cmsoccaddon, commerceorgaddon, customerticketingaddon, smarteditaddon, textfieldconfiguratortemplateaddon]",
			"[ysetplatformproperties] ---------------------------------------------------------------",
			"[ysetplatformproperties] Web root <web> of the extension <testweb> has been explicitely disabled",
			"[ysetplatformproperties] There is no extension with web root set to \"/\".",
			"     [echo] ---------------------------------------------------------------",
			"     [echo] hybris Platform Environment",
			"     [echo] ---------------------------------------------------------------",
			"     [echo] OS family:                 unix",
			"     [echo] Java platform:             OpenJDK Runtime Environment, 21.0.6+7-LTS",
			"     [echo] Java compiler:             modern",
			"     [echo] Build target:              21",
			"     [echo] Compilation mode:          strict",
			"     [echo] hybris Platform directory: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/platform",
			"     [echo] hybris Platform version:   2211-jdk21.11",
			"     [echo] Ant version:               Apache Ant(TM) version 1.10.15 compiled on August 25 2024",
			"     [echo] Ant memory settings:       -Xmx2G -Dfile.encoding=UTF-8 -Dpolyglot.js.nashorn-compat=true -Dpolyglot.engine.WarnInterpreterOnly=false --add-exports java.xml/com.sun.org.apache.xpath.internal=ALL-UNNAMED --add-exports java.xml/com.sun.org.apache.xpath.internal.objects=ALL-UNNAMED",
			"     [echo] ---------------------------------------------------------------",
			"     [echo] Extension file used: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/config/localextensions.xml",
			"     [echo] Platform home: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/platform",
			"     [echo] Log folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/log",
			"     [echo] Data folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/data",
			"     [echo] Config folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/config",
			"     [echo] Bin folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin",
			"     [echo] Temp folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/temp/hybris",
			"     [echo] Boostrap bin folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/platform/bootstrap/bin",
			"     [echo] Roles folder: /Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/platform/../../roles",
			"     [echo] ---------------------------------------------------------------",
			"     [echo] --- Extensions in dependency order ( options: ",
			"     [echo] ---  @deprecated: is deprecated, p: platform extension,*: auto-required ",
			"     [echo] ---  ?: lazy-loaded, i: got items.xml, b: got beans.xml, c: got core module ",
			"     [echo] ---  w: got web module ) ",
			"     [echo] ---------------------------------------------------------------",
			"     [echo] core 2211-jdk21.11 [p*cib]",
			"     [echo] commons 2211-jdk21.11 [p*ci]",
			"     [echo] deliveryzone 2211-jdk21.11 [p*ci]",
			"     [echo] maintenanceweb 2211-jdk21.11 [p*w]",
			"     [echo] mediaweb 2211-jdk21.11 [p*cw]",
			"     [echo] paymentstandard 2211-jdk21.11 [p*ci]",
			"     [echo] scripting 2211-jdk21.11 [p*ci]",
			"     [echo] processing->(scripting,commons) 2211-jdk21.11 [p*cibw]",
			"     [echo] impex->processing 2211-jdk21.11 [p*ci]",
			"     [echo] testweb 2211-jdk21.11 [p*w]",
			"     [echo] validation->impex 2211-jdk21.11 [p*ci]",
			"     [echo] catalog->validation 2211-jdk21.11 [p*cib]",
			"     [echo] advancedsavedquery->catalog 2211-jdk21.11 [p*ci]",
			"     [echo] comments->catalog 2211-jdk21.11 [p*ci]",
			"     [echo] europe1->catalog 2211-jdk21.11 [p*ci]",
			"     [echo] platformservices->(paymentstandard,deliveryzone,europe1) 2211-jdk21.11 [p*cb]",
			"     [echo] hac->platformservices 2211-jdk21.11 [p*cw]",
			"     [echo] oauth2commons->platformservices 2211-jdk21.11 [p*cib]",
			"     [echo] authorizationserver->oauth2commons 2211-jdk21.11 [p*cibw]",
			"     [echo] resourceserver->oauth2commons 2211-jdk21.11 [p*cibw]",
			"     [echo] workflow->(catalog,platformservices) 2211-jdk21.11 [p*ci]",
			"     [echo] alluicommons <-?-(backoffice,smarteditwebservices) 2211-jdk21.11 [?ci] path:/Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/modules/platform/alluicommons",
			"     [echo] apiregistryservices <-?-(basecommerce,inboundservices,outboundservices,apiregistrybackoffice) 2211-jdk21.11 [?cib] path:/Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/modules/api-registry/apiregistryservices",
			"     [echo] @deprecated acceleratorstorefrontcommons->acceleratorfacades <-?-(commerceorgaddon,textfieldconfiguratortemplateaddon,assistedservicestorefront,customerticketingaddon,tyupunchoutaddon,tyub2baccaddon,smarteditaddon,tyustorefront) 2211-jdk21.11 [?cib] path:/Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/modules/core-accelerator/deprecated/acceleratorstorefrontcommons",
			"     [echo] tyustorefront->(captchaaddon,tyupunchoutaddon,tyub2baccaddon,smarteditaddon,tyufacades,productlistservices,acceleratorstorefrontcommons,assistedservicestorefront,resilience4y) 2211-jdk21.11 [w] path:/Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/custom/tyu/tyustorefront",
			"     [echo] swaggerintegration->webservicescommons 2211-jdk21.11 [cib] path:/Users/I303764/SAPDevelop/src/env/abc/def-parts/core-customize/hybris/bin/modules/web-services-commons/swaggerintegration"
	);

	@Test
	public void testParseDirectories() {
		final SystemConfig config = SystemConfig.parse(SAMPLE_LOG);

		Assert.assertNotNull(config.getPlatformHome());
		Assert.assertTrue(config.getPlatformHome().getAbsolutePath().endsWith("bin/platform"));

		Assert.assertNotNull(config.getConfigDir());
		Assert.assertTrue(config.getConfigDir().getAbsolutePath().endsWith("hybris/config"));

		Assert.assertNotNull(config.getBinDir());
		Assert.assertTrue(config.getBinDir().getAbsolutePath().endsWith("hybris/bin"));

		Assert.assertNotNull(config.getDataDir());
		Assert.assertTrue(config.getDataDir().getAbsolutePath().endsWith("hybris/data"));

		Assert.assertNotNull(config.getLogDir());
		Assert.assertTrue(config.getLogDir().getAbsolutePath().endsWith("hybris/log"));

		Assert.assertNotNull(config.getTempDir());
		Assert.assertTrue(config.getTempDir().getAbsolutePath().endsWith("temp/hybris"));

		Assert.assertNotNull(config.getBootstrapBinDir());
		Assert.assertTrue(config.getBootstrapBinDir().getAbsolutePath().endsWith("bootstrap/bin"));

		Assert.assertNotNull(config.getRolesDir());
		Assert.assertNotNull(config.getExtensionFileUsed());
		Assert.assertTrue(config.getExtensionFileUsed().getAbsolutePath().endsWith("localextensions.xml"));
	}

	@Test
	public void testParseEnvironment() {
		final SystemConfig config = SystemConfig.parse(SAMPLE_LOG);

		Assert.assertEquals("unix", config.getOsFamily());
		Assert.assertEquals("2211-jdk21.11", config.getPlatformVersion());
		Assert.assertEquals("21", config.getBuildTarget());
		Assert.assertEquals("strict", config.getCompilationMode());
		Assert.assertEquals("modern", config.getJavaCompiler());
		Assert.assertEquals("OpenJDK Runtime Environment, 21.0.6+7-LTS", config.getJavaPlatform());
		Assert.assertTrue(config.getAntVersion().contains("1.10.15"));
		Assert.assertTrue(config.getAntMemorySettings().contains("-Xmx2G"));
	}

	@Test
	public void testParseDeprecatedExtensions() {
		final SystemConfig config = SystemConfig.parse(SAMPLE_LOG);

		final List<String> deprecated = config.getDeprecatedExtensions();
		Assert.assertEquals(10, deprecated.size());
		Assert.assertTrue(deprecated.contains("acceleratorstorefrontcommons"));
		Assert.assertTrue(deprecated.contains("smarteditaddon"));
	}

	@Test
	public void testParseExtensions() {
		final SystemConfig config = SystemConfig.parse(SAMPLE_LOG);

		Assert.assertFalse(config.getExtensionsInOrder().isEmpty());

		final ExtensionConfig core = config.getExtension("core");
		Assert.assertNotNull(core);
		Assert.assertTrue(core.isPlatformExtension());
		Assert.assertTrue(core.isAutoRequired());
		Assert.assertTrue(core.isCoreModule());
		Assert.assertFalse(core.isWebModule());
		Assert.assertTrue(core.hasItemsXml());
		Assert.assertTrue(core.hasBeansXml());

		final ExtensionConfig processing = config.getExtension("processing");
		Assert.assertNotNull(processing);
		Assert.assertTrue(processing.isCoreModule());
		Assert.assertTrue(processing.isWebModule());
		Assert.assertTrue(processing.getDependencies().contains("scripting"));
		Assert.assertTrue(processing.getDependencies().contains("commons"));

		final ExtensionConfig alluicommons = config.getExtension("alluicommons");
		Assert.assertNotNull(alluicommons);
		Assert.assertTrue(alluicommons.isLazyLoaded());
		Assert.assertTrue(alluicommons.getRequiredBy().contains("backoffice"));
		Assert.assertTrue(alluicommons.getRequiredBy().contains("smarteditwebservices"));
		Assert.assertNotNull(alluicommons.getExtensionDirectory());

		final ExtensionConfig accStorefront = config.getExtension("acceleratorstorefrontcommons");
		Assert.assertNotNull(accStorefront);
		Assert.assertTrue(accStorefront.isDeprecated());
		Assert.assertTrue(accStorefront.getDependencies().contains("acceleratorfacades"));

		final ExtensionConfig storefront = config.getExtension("tyustorefront");
		Assert.assertNotNull(storefront);
		Assert.assertTrue(storefront.isWebModule());
		Assert.assertFalse(storefront.isCoreModule());
	}

	@Test
	public void testReplaceProperties() {
		final SystemConfig config = SystemConfig.parse(SAMPLE_LOG);

		final String replaced = config.replaceProperties("${platformhome}/ext");
		Assert.assertNotNull(replaced);
		Assert.assertFalse(replaced.contains("${platformhome}"));
		Assert.assertTrue(replaced.endsWith("bin/platform/ext"));
	}
}
