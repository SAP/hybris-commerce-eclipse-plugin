/*******************************************************************************
 * Copyright 2020 SAP
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ******************************************************************************/
package com.hybris.hyeclipse.impex.lsp;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.hybris.hyeclipse.commons.Activator;
import com.hybris.hyeclipse.commons.Constants;
import com.hybris.hyeclipse.commons.typesystem.TypeSystemSnapshot;
import com.hybris.hyeclipse.commons.typesystem.YAttributeDescriptor;
import com.hybris.hyeclipse.commons.typesystem.YType;
import com.hybris.hyeclipse.impex.ls.types.TypeCatalog;
import com.hybris.hyeclipse.impex.ls.types.TypeSystemProvider;

/** Maps documents to the single currently indexed platform without global fallback or source parsing. */
public final class WorkspaceTypeSystemProvider implements TypeSystemProvider {

	private final IWorkspace workspace;
	private final Supplier<TypeSystemSnapshot> snapshots;
	private TypeSystemSnapshot previous;
	private TypeCatalog cached = TypeCatalog.empty();

	/** Uses the shared, non-blocking generated-model index. */
	public WorkspaceTypeSystemProvider() {
		this(ResourcesPlugin.getWorkspace(), () -> Activator.getDefault() == null ? TypeSystemSnapshot.empty()
				: Activator.getDefault().getTypeSystemIndex().getSnapshot());
	}

	/**
	 * @param workspace owning workspace
	 * @param snapshots non-blocking supplier of the indexed platform's latest snapshot
	 */
	public WorkspaceTypeSystemProvider(final IWorkspace workspace, final Supplier<TypeSystemSnapshot> snapshots) {
		this.workspace = workspace;
		this.snapshots = snapshots;
	}

	@Override
	public synchronized TypeCatalog getCatalog(final String documentUri) {
		if (documentRoot(documentUri) == null) {
			return TypeCatalog.empty();
		}
		final TypeSystemSnapshot snapshot = snapshots.get();
		if (snapshot == null || snapshot.getTypes().isEmpty()) {
			return TypeCatalog.empty();
		}
		if (snapshot != previous) {
			final List<TypeCatalog.Type> types = new ArrayList<>();
			for (final YType type : snapshot.getTypes()) {
				final List<TypeCatalog.Attribute> attributes = new ArrayList<>();
				for (final YAttributeDescriptor attribute : type.getAttributes()) {
					attributes.add(new TypeCatalog.Attribute(attribute.getQualifier(), attribute.getJavaType(),
							attribute.getSourcePath(), attribute.getSourceLine()));
				}
				types.add(new TypeCatalog.Type(type.getCode(), type.getJavaClassName(), type.getSourcePath(),
						type.getSourceLine(), attributes));
			}
			cached = new TypeCatalog(types);
			previous = snapshot;
		}
		return cached;
	}

	@Override
	public Location resolveLocation(final String documentUri, final String sourcePath, final int sourceLine) {
		final Path root = documentRoot(documentUri);
		if (root == null || sourcePath == null || sourceLine < 1) {
			return null;
		}
		final IFile file;
		try {
			final IPath path = org.eclipse.core.runtime.Path.fromPortableString(sourcePath);
			if (!path.isAbsolute() || path.segmentCount() < 2) {
				return null;
			}
			file = workspace.getRoot().getFile(path);
		} catch (final IllegalArgumentException e) {
			return null;
		}
		if (!file.isAccessible() || file.getLocation() == null || file.getLocationURI() == null
				|| !file.getLocation().toFile().toPath().toAbsolutePath().normalize().startsWith(root)) {
			return null;
		}
		final Position line = new Position(sourceLine - 1, 0);
		return new Location(file.getLocationURI().toString(), new Range(line, new Position(sourceLine - 1, 0)));
	}

	private Path documentRoot(final String documentUri) {
		if (documentUri == null) {
			return null;
		}
		final IProject platform = workspace.getRoot().getProject(Constants.PLATFROM);
		if (!platform.isAccessible() || platform.getLocation() == null) {
			return null;
		}
		final URI uri;
		try {
			uri = URI.create(documentUri);
		} catch (final IllegalArgumentException e) {
			return null;
		}
		if (!"file".equals(uri.getScheme()) || uri.isOpaque() || uri.getPath() == null //$NON-NLS-1$
				|| uri.getQuery() != null || uri.getFragment() != null) {
			return null;
		}
		final IFile[] files = workspace.getRoot().findFilesForLocationURI(uri);
		if (files.length != 1 || !files[0].isAccessible() || files[0].getLocation() == null) {
			return null;
		}
		Path root = platform.getLocation().toFile().toPath().toAbsolutePath().normalize();
		// Only the standard hybris/bin/platform layout establishes a wider Commerce tree.
		if (root.getFileName().toString().equals(Constants.PLATFROM) && root.getParent() != null
				&& root.getParent().getParent() != null && root.getParent().getParent().getParent() != null
				&& "bin".equals(root.getParent().getFileName().toString())) { //$NON-NLS-1$
			root = root.getParent().getParent();
		}
		return root != null && files[0].getLocation().toFile().toPath().toAbsolutePath().normalize().startsWith(root)
				? root : null;
	}
}
