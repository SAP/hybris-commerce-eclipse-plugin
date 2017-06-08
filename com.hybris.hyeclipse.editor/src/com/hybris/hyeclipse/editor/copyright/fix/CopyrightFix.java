package com.hybris.hyeclipse.editor.copyright.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import com.hybris.hyeclipse.editor.copyright.manager.CopyrightManager;

/**
 * Implementation of {@link ICleanUpFix} handling Copyright fixes
 */
public class CopyrightFix implements ICleanUpFix {

	private final static CopyrightManager copyrightManager = new CopyrightManager();
	private final CompilationUnitChange change;

	protected CopyrightFix(final CompilationUnitChange change) {
		this.change = change;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CompilationUnitChange createChange(final IProgressMonitor monitor) throws CoreException {
		return change;
	}

	/**
	 * Creates {@link CopyrightFix} according to preferences
	 *
	 * @param compilationUnit
	 *            compilation unit for which fix is applied
	 * @param enabled
	 *            information if copyright fix is enabled
	 * @param override
	 *            information if copyrights should be overriden
	 * @return copyright fix; null if fix was not applied
	 */
	public static CopyrightFix createCleanUp(final CompilationUnit compilationUnit, final boolean enabled,
			final boolean override) {
		if (!enabled) {
			return null;
		}
		if (!copyrightManager.hasCopyrightsComment(compilationUnit)) {
			return new CopyrightFix(copyrightManager.addCopyrightsHeader(compilationUnit));
		} else if (override) {
			return new CopyrightFix(copyrightManager.replaceCopyrightsHeader(compilationUnit));
		}
		return null;
	}

}
