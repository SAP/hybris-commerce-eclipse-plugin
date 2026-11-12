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
package com.hybris.yps.hyeclipse.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hybris.hyeclipse.commons.config.ExtensionConfig;
import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.hyeclipse.commons.config.SystemConfigRepository;
import com.hybris.hyeclipse.commons.config.SystemConfigXmlSerializer;

public class SystemConfigRepositoryTest {

	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void provisionalImportIsSharedButDoesNotReplaceCommittedDataOnFailure() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File state = temporary.newFolder("state");
		final SystemConfigRepository repository = new SystemConfigRepository(state);
		repository.save(config(platform, "2211"));
		final SystemConfig pending = config(platform, "2211.1");
		try (final SystemConfigRepository.ImportSnapshot snapshot = repository.beginImport(pending)) {
			Assert.assertSame(pending, repository.find(platform).get());
			Assert.assertEquals("2211", new SystemConfigRepository(state).find(platform).get().getPlatformVersion());
			Assert.assertThrows(IllegalStateException.class, () -> repository.beginImport(pending));
		}
		Assert.assertEquals("2211", repository.find(platform).get().getPlatformVersion());
		try (final SystemConfigRepository.ImportSnapshot snapshot = repository.beginImport(pending)) {
			repository.save(pending);
		}
		Assert.assertSame(pending, repository.find(platform).get());
		Assert.assertEquals("2211.1", new SystemConfigRepository(state).find(platform).get().getPlatformVersion());
	}

	@Test
	public void restoresAllFieldsAndExtensionOrderAfterRestart() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File state = temporary.newFolder("state");
		final SystemConfig original = config(platform, "2211");
		final SystemConfigRepository repository = new SystemConfigRepository(state);
		repository.save(original);
		Assert.assertSame(original, repository.find(platform).get());
		final SystemConfig restored = new SystemConfigRepository(state).find(platform).get();
		Assert.assertEquals(properties(original), properties(restored));
		Assert.assertSame(restored.getExtensionsInOrder().get(1), restored.getExtension("custom"));
		Assert.assertEquals("core", restored.getExtensionsInOrder().get(0).getName());
		Assert.assertEquals(platform.getParentFile().getCanonicalFile(), restored.getBinDir());
		Assert.assertEquals(new File(platform.getParentFile().getParentFile(), "config").getCanonicalFile(),
				restored.getConfigDir());
		Assert.assertEquals(Arrays.asList("core", "commons"), restored.getExtension("custom").getDependencies());
	}

	@Test
	public void isolatesWorkspacesAndPlatformsAndSupportsRemoval() throws Exception {
		final File first = temporary.newFolder("first", "bin", "platform");
		final File second = temporary.newFolder("second", "bin", "platform");
		final File state = temporary.newFolder("state");
		final SystemConfigRepository repository = new SystemConfigRepository(state);
		repository.save(config(first, "2211"));
		repository.save(config(second, "6.7"));
		final SystemConfigRepository reopened = new SystemConfigRepository(state);
		Assert.assertEquals("2211", reopened.find(new File(first, ".")).get().getPlatformVersion());
		Assert.assertEquals("6.7", reopened.find(second).get().getPlatformVersion());
		Assert.assertFalse(new SystemConfigRepository(temporary.newFolder("other-state")).find(first).isPresent());
		reopened.remove(first);
		Assert.assertFalse(reopened.find(first).isPresent());
		Assert.assertFalse(new SystemConfigRepository(state).find(first).isPresent());
		Assert.assertTrue(reopened.find(second).isPresent());
	}

	@Test
	public void rejectsCorruptSnapshotsAndCanRecoverThroughImport() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File state = temporary.newFolder("state");
		new SystemConfigRepository(state).save(config(platform, "2211"));
		final File[] files = new File(state, "system-configs").listFiles();
		Assert.assertNotNull(files);
		Assert.assertEquals(1, files.length);
		Files.writeString(files[0].toPath(), "not XML", StandardCharsets.UTF_8);
		final SystemConfigRepository reopened = new SystemConfigRepository(state);
		Assert.assertThrows(CoreException.class, () -> reopened.find(platform));
		reopened.save(config(platform, "2211.1"));
		Assert.assertEquals("2211.1", new SystemConfigRepository(state).find(platform).get().getPlatformVersion());
	}

	@Test
	public void rejectsUnsupportedSchema() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final Properties xml = properties(config(platform, "2211"));
		xml.setProperty("schema.version", "999");
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		xml.storeToXML(output, null);
		Assert.assertThrows(IOException.class, () -> new SystemConfigXmlSerializer()
				.read(new ByteArrayInputStream(output.toByteArray())));
	}

	@Test
	public void failedSavePreservesPreviousSnapshot() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File state = temporary.newFolder("state");
		final SystemConfigRepository repository = new SystemConfigRepository(state);
		repository.save(config(platform, "2211"));
		Assert.assertThrows(IllegalArgumentException.class,
				() -> repository.save(SystemConfig.parse(Arrays.asList("[echo] Platform home: " + platform))));
		Assert.assertEquals("2211", repository.find(platform).get().getPlatformVersion());
		Assert.assertEquals("2211", new SystemConfigRepository(state).find(platform).get().getPlatformVersion());
		final File blockedState = temporary.newFile("not-a-directory");
		Assert.assertThrows(CoreException.class,
				() -> new SystemConfigRepository(blockedState).save(config(platform, "2211.1")));
	}

	@Test
	public void unavailablePlatformIsNotReturnedFromCache() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final SystemConfigRepository repository = new SystemConfigRepository(temporary.newFolder("state"));
		repository.save(config(platform, "2211"));
		Files.delete(platform.toPath());
		Assert.assertFalse(repository.find(platform).isPresent());
	}

	@Test
	public void persistsDescriptorMetadataWithoutReadingItAgain() throws Exception {
		final File platform = temporary.newFolder("hybris", "bin", "platform");
		final File extension = temporary.newFolder("custom");
		final File descriptor = new File(extension, "extensioninfo.xml");
		Files.writeString(descriptor.toPath(), "<extensioninfo><extension name=\"custom\">"
				+ "<coremodule packageroot=\"com.example.custom\"/><hmcmodule/>"
				+ "<meta key=\"backoffice-module\" value=\"true\"/></extension></extensioninfo>");
		final SystemConfig config = SystemConfig.parse(Arrays.asList("[echo] Platform home: " + platform,
				"[echo] hybris Platform version: 2211", "[echo] custom 2211 [cw] path:" + extension))
				.withExtensionMetadata();
		final File state = temporary.newFolder("state");
		new SystemConfigRepository(state).save(config);
		Files.delete(descriptor.toPath());
		final ExtensionConfig restored = new SystemConfigRepository(state).find(platform).get().getExtension("custom");
		Assert.assertEquals("com.example.custom", restored.getPackageRoot());
		Assert.assertTrue(restored.isHmcModule());
		Assert.assertTrue(restored.isBackofficeModule());
	}

	private SystemConfig config(final File platform, final String version) {
		return SystemConfig.parse(Arrays.asList("[echo] Platform home: " + platform,
				"[echo] hybris Platform version: " + version, "[echo] OS family: unix",
				"[echo] Java platform: JDK 21", "[echo] Java compiler: modern", "[echo] Build target: 21",
				"[echo] Compilation mode: strict", "[echo] Ant version: 1.10", "[echo] Ant memory settings: -Xmx2G",
				"[echo] Roles folder: " + new File(platform, "roles"), "[echo] Active role: default",
				"[echo] Active role instance: node1", "[echo] Extension file used: " + new File(platform, "extensions.xml"),
				"[echo] Extra property: value & <xml> ü", "Deprecated extensions: [custom]",
				"[echo] core " + version + " [p*cib]",
				"[echo] @deprecated custom->(core,commons) <-?-(other,next) " + version + " [?cibw] path:"
						+ new File(platform.getParentFile(), "custom")));
	}

	private Properties properties(final SystemConfig config) throws IOException {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		new SystemConfigXmlSerializer().write(config, output);
		final Properties result = new Properties();
		result.loadFromXML(new ByteArrayInputStream(output.toByteArray()));
		return result;
	}
}
