package com.hybris.hyeclipse.platform;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

final class WorkspaceChangedListener implements IResourceChangeListener {
	
	private final PlatformHolder platformHolder;
	
	WorkspaceChangedListener(PlatformHolder platformHolder) {
		this.platformHolder = platformHolder;
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		platformHolder.workspaceChanged(event);
	}

}