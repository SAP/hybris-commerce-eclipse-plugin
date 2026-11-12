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
package com.hybris.hyeclipse.tsvextended.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.hybris.hyeclipse.commons.config.ExtensionConfig;
import com.hybris.hyeclipse.commons.config.SystemConfig;
import com.hybris.hyeclipse.commons.validation.ValidationService;
import com.hybris.hyeclipse.commons.validation.ValidationXml;
import com.hybris.hyeclipse.platform.Platform;
import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsv.Messages;
import com.hybris.hyeclipse.tsvextended.utils.ExtensionHolder;

/** Extended analysis backed by shared configuration and source-built schema rules. */
public class TSVExtendedAnalyser {
	private final Shell shell;
	private final Set<ExtensionHolder> selected = new LinkedHashSet<>();
	private final Set<IExtensionListViewer> listeners = new LinkedHashSet<>();
	private Set<ExtensionHolder> extensions;
	private SystemConfig config;
	private String resultsString;

	/** @param composite owning UI composite */
	public TSVExtendedAnalyser(final Composite composite) {
		shell = composite.getShell();
		if (!Platform.holder.isPlatformAvailable()) {
			return;
		}
		try {
			new ProgressMonitorDialog(shell).run(true, true, monitor -> {
				monitor.beginTask(Messages.RunTSVWizard_taskCreating, IProgressMonitor.UNKNOWN);
				try {
					config = com.hybris.hyeclipse.commons.Activator.getDefault().getSystemConfigRepository()
							.find(Platform.holder.getCurrent().getPlatformHomeFile())
							.orElseThrow(() -> new IllegalStateException("Import the platform before running TSV"));
					extensions = new LinkedHashSet<>();
					for (final ExtensionConfig extension : config.getExtensionsInOrder()) {
						if (extension.getExtensionDirectory() == null || ValidationService.itemsFile(extension) == null) {
							continue;
						}
						final ExtensionHolder holder = new ExtensionHolder(extension);
						holder.setName(extension.getName());
						final boolean template = extension.getName().startsWith("y")
								&& new File(extension.getExtensionDirectory(), "extgen.properties").isFile();
						final boolean platform = extension.isPlatformExtension() || extension.getExtensionDirectory()
								.toPath().startsWith(config.getBinDir().toPath().resolve("modules"));
						holder.setSelected(!template && !platform);
						extensions.add(holder);
						if (holder.isSelected()) {
							selected.add(holder);
						}
					}
				} catch (CoreException | RuntimeException e) { throw new InvocationTargetException(e); }
				finally { monitor.done(); }
			});
		} catch (InvocationTargetException e) {
			extensions = null;
			Activator.logError(Messages.RunTSVWizard_error, e);
		} catch (InterruptedException e) { extensions = null; Thread.currentThread().interrupt(); }
	}

	/** @return available imported extensions, or null when no configuration is available */
	public Set<ExtensionHolder> getAllScannableExtensions() { return extensions; }
	/** @return selected extension metadata */
	public List<ExtensionConfig> getAllExtensionsToScan() {
		final List<ExtensionConfig> result = new ArrayList<>();
		for (final ExtensionHolder holder : selected) {
			result.add(holder.getExtensionInfo());
		}
		return result;
	}
	/** @param extension changed selection */
	public void extensionChanged(final ExtensionHolder extension) {
		if (extension.isSelected()) {
			selected.add(extension);
		} else {
			selected.remove(extension);
		}
		for (final IExtensionListViewer listener : listeners) {
			listener.updateExtensionList(extension);
		}
	}
	/** @param listener listener to remove */
	public void removeChangeListener(final IExtensionListViewer listener) { listeners.remove(listener); }
	/** @param listener listener to register */
	public void addChangeListener(final IExtensionListViewer listener) { listeners.add(listener); }

	protected void runTSVAnalysis() {
		resultsString = null;
		final List<File> files = new ArrayList<>();
		for (final ExtensionConfig extension : getAllExtensionsToScan()) {
			final File file = ValidationService.itemsFile(extension);
			if (file != null) {
				files.add(file);
			}
		}
		try {
			new ProgressMonitorDialog(shell).run(true, true, monitor -> {
				try {
					monitor.beginTask(Messages.RunTSVWizard_taskCreating, IProgressMonitor.UNKNOWN);
					resultsString = ValidationXml.report(new ValidationService().analyze(config, files, monitor));
				} finally { monitor.done(); }
			});
		} catch (InvocationTargetException e) {
			Activator.logError(Messages.RunTSVWizard_error, e);
		} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	/** @return generated TSV report XML */
	public String getResultsString() { return resultsString; }
}
