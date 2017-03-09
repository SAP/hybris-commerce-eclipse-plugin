package com.hybris.hyeclipse.tsv.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

public class TSVResultsStorage extends PlatformObject implements IStorage {
	
	private String resultsString;
	private IPath path;
	
	public TSVResultsStorage(String resultsString, IPath path) {
		this.resultsString = resultsString;
		this.path = path;
	}

	@Override
	public InputStream getContents() throws CoreException {
		
		return new ByteArrayInputStream(resultsString.getBytes());
	}
	
	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public IPath getFullPath() {
		return path;
	}

	@Override
	public String getName() {
		return "analysis.tsv";
	}
	
}
