package com.hybris.hyeclipse.tsv.builder;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import com.hybris.hyeclipse.tsv.validator.ItemsXmlValidator;
import com.hybris.ps.tsv.results.IResult;
import com.hybris.ps.tsv.results.ResultState;
import com.hybris.ps.tsv.rules.xml.RulePriority;

abstract class TsvBuilder extends IncrementalProjectBuilder {

	private static final Map<RulePriority, Integer> priorityMap = new EnumMap<>(RulePriority.class);
	static {
		priorityMap.put(RulePriority.H, IMarker.PRIORITY_HIGH);
		priorityMap.put(RulePriority.M, IMarker.PRIORITY_NORMAL);
		priorityMap.put(RulePriority.L, IMarker.PRIORITY_LOW);
	}

	class ItemsXmlFinder implements IResourceDeltaVisitor, IResourceVisitor {
		final Collection<IFile> files = new LinkedHashSet<>();

		@Override
		public boolean visit(final IResourceDelta delta) throws CoreException {
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				return visit(delta.getResource());
			}
			return true;
		}
		
		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				final IFile file = (IFile) resource;
				if (file.getName().endsWith("items.xml")) {
					files.add(file);
					return false;
				}
			}

			return true;
		}
	}

	public static final String MARKER_TYPE = "com.hybris.hyeclipse.tsv.tsvproblem";

	@Override
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {
		//TODO find referenced extensions and their eclipse projects and return them
		if (kind == CLEAN_BUILD) {
			clean(monitor);
		}
		else if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}
	
	protected abstract ItemsXmlValidator getValidator() throws CoreException;

	void checkTsv(final Collection<IFile> files, IProgressMonitor monitor) throws CoreException {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, files.size() * 5);
		final ItemsXmlValidator validator = getValidator();
		for (final IFile file : files) {
			if (subMonitor.isCanceled()) {
				throw new OperationCanceledException("TSV Check cancelled");
			}
			subMonitor.subTask("Checking typesystem: " + file.getFullPath());
			
			processResults(file, validator.analyze(file, subMonitor.newChild(4)), subMonitor.newChild(1));
			
		}
	}

	private void processResults(final IFile file, final Collection<IResult> results, final IProgressMonitor monitor) throws CoreException {
        final SubMonitor progress = SubMonitor.convert(monitor, "Processing results", results.size());
        deleteMarkers(file);
        for (final IResult result : results) {
        	if (result.getState() == ResultState.FAIL || result.getState() == ResultState.ERROR) {
				addMarker(file, result.getDescription(), result.getLine() != null ? result.getLine().intValue() : 1, IMarker.SEVERITY_ERROR,
						priorityMap.get(result.getRule().getPriority()));
        	}
        	progress.worked(1);
        }
	}

	private void addMarker(final IFile file, String message, int lineNumber, int severity, int priority) throws CoreException {
		final IMarker marker = file.createMarker(TsvBuilder.MARKER_TYPE);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		if (lineNumber == -1) {
			lineNumber = 1;
		}
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		marker.setAttribute(IMarker.PRIORITY, priority);
	}
	
	private void deleteMarkers(IFile file) throws CoreException {
		file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		final ItemsXmlFinder finder = new ItemsXmlFinder();
		getProject().accept(finder);
		checkTsv(finder.files, monitor);
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		final ItemsXmlFinder finder = new ItemsXmlFinder();
		delta.accept(finder);
		checkTsv(finder.files, monitor);
	}
}
