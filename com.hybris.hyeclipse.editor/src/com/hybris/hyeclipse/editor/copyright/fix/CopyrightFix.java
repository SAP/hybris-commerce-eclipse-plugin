package com.hybris.hyeclipse.editor.copyright.fix;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import com.hybris.hyeclipse.editor.copyright.manager.CopyrightManager;

public class CopyrightFix implements ICleanUpFix {

	private final static CopyrightManager copyrightManager = new CopyrightManager();

	@Override
	public CompilationUnitChange createChange(final IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public static CopyrightFix createCleanUp(final CompilationUnit compilationUnit, final boolean enabled,
			final boolean override) {
		if (enabled) {
			if (!hasCopyrightsComment(compilationUnit)) {
				copyrightManager.addCopyrightsHeader(compilationUnit);
			} else if (override) {
				copyrightManager.replaceCopyrightsHeader(compilationUnit);
			}
			return null;
		}
		return null;
	}

	private static boolean hasCopyrightsComment(final CompilationUnit compilationUnit) {
		@SuppressWarnings("unchecked")
		final List<Comment> comments = compilationUnit.getCommentList();
		if (comments.isEmpty()) {
			return false;
		}
		final PackageDeclaration packageNode = compilationUnit.getPackage();
		final boolean commentBeforePackage = comments.get(0).getStartPosition() < packageNode.getStartPosition();
		final boolean hasJavaDoc = packageNode.getJavadoc() != null;
		return commentBeforePackage || hasJavaDoc;
	}
}
