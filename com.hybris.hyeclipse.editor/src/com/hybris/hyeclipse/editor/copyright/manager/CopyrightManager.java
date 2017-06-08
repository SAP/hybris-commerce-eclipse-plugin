package com.hybris.hyeclipse.editor.copyright.manager;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.hybris.hyeclipse.editor.Activator;
import com.hybris.hyeclipse.editor.preferences.EditorPreferenceConstants;
import com.hybris.hyeclipse.hac.utils.ConsoleUtils;

public class CopyrightManager {

	private static final String ADD_COPYRIGHT = "Add Copyright";
	private static final String OVERRIDE_COPYRIGHT = "Override Copyright";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private ASTRewrite rewriter;
	private CompilationUnitChange change;

	public String getCopyrightText() {
		final String[] contents = Activator.getDefault().getPreferenceStore()
				.getString(EditorPreferenceConstants.COPYRIGHT_CONTENT).split(NEW_LINE_SEPARATOR);
		final String firstLine = Activator.getDefault().getPreferenceStore()
				.getString(EditorPreferenceConstants.COPYRIGHT_FIRST_LINE);
		final String prefix = Activator.getDefault().getPreferenceStore()
				.getString(EditorPreferenceConstants.COPYRIGHT_LINE_PREFIX);
		final String lastLine = Activator.getDefault().getPreferenceStore()
				.getString(EditorPreferenceConstants.COPYRIGHT_LAST_LINE);
		final StringBuilder copyrightBuilder = new StringBuilder();
		copyrightBuilder.append(firstLine + NEW_LINE_SEPARATOR);
		for (final String line : contents) {
			copyrightBuilder.append(prefix + line + NEW_LINE_SEPARATOR);
		}
		copyrightBuilder.append(lastLine + NEW_LINE_SEPARATOR);
		return copyrightBuilder.toString();
	}

	public CompilationUnitChange addCopyrightsHeader(final CompilationUnit compilationUnit) {
		final ICompilationUnit unit = (ICompilationUnit) compilationUnit.getJavaElement();
		change = new CompilationUnitChange(ADD_COPYRIGHT, unit);
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		final ListRewrite listRewrite = rewriter.getListRewrite(compilationUnit.getPackage(),
				PackageDeclaration.ANNOTATIONS_PROPERTY);
		final Comment placeHolder = (Comment) rewriter.createStringPlaceholder(getCopyrightText(),
				ASTNode.BLOCK_COMMENT);
		listRewrite.insertFirst(placeHolder, null);
		rewriteCompilationUnit(unit, getNewUnitSource(unit, null));
		return change;
	}

	public CompilationUnitChange replaceCopyrightsHeader(final CompilationUnit compilationUnit) {
		final ICompilationUnit unit = (ICompilationUnit) compilationUnit.getJavaElement();
		change = new CompilationUnitChange(OVERRIDE_COPYRIGHT, unit);
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		final List<Comment> comments = getCommentList(compilationUnit);
		final Comment copyrightComment = comments.get(0);
		rewriteCompilationUnit(unit, getNewUnitSource(unit, copyrightComment));
		return change;
	}

	public boolean hasCopyrightsComment(final CompilationUnit compilationUnit) {
		final List<Comment> comments = getCommentList(compilationUnit);
		if (comments.isEmpty()) {
			return false;
		}
		final PackageDeclaration packageNode = compilationUnit.getPackage();
		final boolean commentBeforePackage = comments.get(0).getStartPosition() < packageNode.getStartPosition();
		final boolean hasJavaDoc = packageNode.getJavadoc() != null;
		return commentBeforePackage || hasJavaDoc;
	}

	private List<Comment> getCommentList(final CompilationUnit compilationUnit) {
		@SuppressWarnings("unchecked")
		final List<Comment> comments = compilationUnit.getCommentList();
		return comments;
	}

	private String getNewUnitSource(final ICompilationUnit unit, final Comment comment) {
		try {
			final String source = unit.getSource();
			if (comment != null) {
				final int endOfComment = comment.getLength();
				return source.replace(source.substring(0, endOfComment + 1), getCopyrightText());
			}
			return source;
		} catch (final JavaModelException e) {
			ConsoleUtils.printError(e.getMessage());
		}
		return null;
	}

	private void rewriteCompilationUnit(final ICompilationUnit unit, final String source) {
		try {
			final TextEdit edits = rewriter.rewriteAST();
			final Document document = new Document(source);
			edits.apply(document);
			unit.getBuffer().setContents(document.get());
			change.setEdit(edits);
		} catch (final JavaModelException | MalformedTreeException | BadLocationException e) {
			ConsoleUtils.printError(e.getMessage());
		}
	}
}
