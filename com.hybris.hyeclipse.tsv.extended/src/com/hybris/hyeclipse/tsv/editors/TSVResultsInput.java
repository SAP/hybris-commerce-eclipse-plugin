package com.hybris.hyeclipse.tsv.editors;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class TSVResultsInput extends PlatformObject implements IStorageEditorInput {
	
	private IStorage storage;
	private String tooltipText = "TSV Extended Analysis";

	public TSVResultsInput(IStorage storage) {
		this.storage = storage;
	}
	
	public TSVResultsInput(IStorage storage, String tooltipText) {
		this.storage = storage;
		this.tooltipText = tooltipText;
	}

	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public IStorage getStorage() throws CoreException {
		return storage;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "Raw";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return tooltipText;
	}

}
