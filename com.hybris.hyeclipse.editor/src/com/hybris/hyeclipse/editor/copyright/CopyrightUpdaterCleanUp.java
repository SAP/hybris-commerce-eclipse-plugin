package com.hybris.hyeclipse.editor.copyright;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import com.hybris.hyeclipse.editor.copyright.constant.CopyrightConstants;
import com.hybris.hyeclipse.editor.copyright.fix.CopyrightFix;

public class CopyrightUpdaterCleanUp implements ICleanUp {

	private static final String UPDATE_COPYRIGHTS_DESCRIPTION = "Update Copyrights";
	private static final String OVERRIDE_COPYRIGHTS_DESCRIPTION = "Override existing copyrights";
	private CleanUpOptions options;
	private RefactoringStatus status;

	@Override
	public RefactoringStatus checkPostConditions(final IProgressMonitor monitor) throws CoreException {
		try {
			if (status == null || status.isOK()) {
				return new RefactoringStatus();
			} else {
				return status;
			}
		} finally {
			status = null;
		}
	}

	@Override
	public RefactoringStatus checkPreConditions(final IJavaProject project, final ICompilationUnit[] compliationUnits,
			final IProgressMonitor monitor) throws CoreException {
		if (options.isEnabled(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS)) {
			status = new RefactoringStatus();
		}
		if (options.isEnabled(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS)
				&& options.isEnabled(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS)) {
			status = new RefactoringStatus();
		}
		return new RefactoringStatus();
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext cleanUpCtx) throws CoreException {
		final CompilationUnit compilationUnit = cleanUpCtx.getAST();
		if (compilationUnit == null) {
			return null;
		}
		return CopyrightFix.createCleanUp(compilationUnit,
				options.isEnabled(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS),
				options.isEnabled(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS));
	}

	@Override
	public CleanUpRequirements getRequirements() {
		final boolean changedRegionsRequired = true;
		final boolean isUpdateCopyrights = options.isEnabled(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS);
		return new CleanUpRequirements(isUpdateCopyrights, isUpdateCopyrights, changedRegionsRequired, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (options.isEnabled(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS)) {
			if (options.isEnabled(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS)) {
				return new String[] { UPDATE_COPYRIGHTS_DESCRIPTION, OVERRIDE_COPYRIGHTS_DESCRIPTION };
			} else {
				return new String[] { UPDATE_COPYRIGHTS_DESCRIPTION };
			}
		}
		return null;
	}

	@Override
	public void setOptions(final CleanUpOptions options) {
		Assert.isLegal(options != null);
		Assert.isTrue(this.options == null);
		this.options = options;
	}

}
