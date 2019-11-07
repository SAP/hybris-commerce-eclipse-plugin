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
import com.hybris.hyeclipse.commons.utils.ConsoleUtils;
import com.hybris.hyeclipse.editor.Activator;
import com.hybris.hyeclipse.editor.preferences.CopyrightPreferenceConstants;

/**
 * Class handling Copyright operations
 */
public class CopyrightManager {

	private static final String ADD_COPYRIGHT = "Add Copyright";
	private static final String OVERRIDE_COPYRIGHT = "Override Copyright";
	private static final String NEW_LINE_SEPARATOR = "\n";

	private ASTRewrite rewriter;
	private CompilationUnitChange change;

	/**
	 * Gets copyright text from preference page
	 *
	 * @return copyright text
	 */
	public String getCopyrightText() {
		final String[] contents = Activator.getDefault().getPreferenceStore()
				.getString(CopyrightPreferenceConstants.COPYRIGHT_CONTENT).split(NEW_LINE_SEPARATOR);
		final String firstLine = Activator.getDefault().getPreferenceStore()
				.getString(CopyrightPreferenceConstants.COPYRIGHT_FIRST_LINE);
		final String prefix = Activator.getDefault().getPreferenceStore()
				.getString(CopyrightPreferenceConstants.COPYRIGHT_LINE_PREFIX);
		final String lastLine = Activator.getDefault().getPreferenceStore()
				.getString(CopyrightPreferenceConstants.COPYRIGHT_LAST_LINE);
		final StringBuilder copyrightBuilder = new StringBuilder();
		copyrightBuilder.append(firstLine).append(NEW_LINE_SEPARATOR);
		for (final String line : contents) {
			copyrightBuilder.append(prefix).append(line).append(NEW_LINE_SEPARATOR);
		}
		copyrightBuilder.append(lastLine);
		return copyrightBuilder.toString();
	}

	/**
	 * Adds copyright header to the compilation unit
	 *
	 * @param compilationUnit
	 *            compilation unit affected
	 * @return compilation unit change
	 */
	public CompilationUnitChange addCopyrightsHeader(final CompilationUnit compilationUnit) {
		final ICompilationUnit unit = (ICompilationUnit) compilationUnit.getJavaElement();
		change = new CompilationUnitChange(ADD_COPYRIGHT, unit);
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		final ListRewrite listRewrite = rewriter.getListRewrite(compilationUnit.getPackage(),
				PackageDeclaration.ANNOTATIONS_PROPERTY);
		final Comment placeHolder = (Comment) rewriter.createStringPlaceholder(getCopyrightText() + NEW_LINE_SEPARATOR,
				ASTNode.BLOCK_COMMENT);
		listRewrite.insertFirst(placeHolder, null);
		rewriteCompilationUnit(unit, getNewUnitSource(unit, null));
		return change;
	}

	/**
	 * Replaces copyright header to the compilation unit
	 *
	 * @param compilationUnit
	 *            compilation unit affected
	 * @return compilation unit change
	 */
	public CompilationUnitChange replaceCopyrightsHeader(final CompilationUnit compilationUnit) {
		final ICompilationUnit unit = (ICompilationUnit) compilationUnit.getJavaElement();
		change = new CompilationUnitChange(OVERRIDE_COPYRIGHT, unit);
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		final List<Comment> comments = getCommentList(compilationUnit);
		Comment copyrightComment = null;
		if (!comments.isEmpty()) {
			copyrightComment = comments.get(0);
		}
		rewriteCompilationUnit(unit, getNewUnitSource(unit, copyrightComment));
		return change;
	}

	/**
	 * Checks whether {@link CompilationUnit} has copyright header
	 *
	 * @param compilationUnit
	 *            checked compilation unit
	 * @return true if {@link CompilationUnit} has copyright header
	 */
	public boolean hasCopyrightsComment(final CompilationUnit compilationUnit) {
		final List<Comment> comments = getCommentList(compilationUnit);
		boolean hasCopyrights = false;
		if (!comments.isEmpty()) {
			final PackageDeclaration packageNode = compilationUnit.getPackage();
			final boolean commentBeforePackage = comments.get(0).getStartPosition() <= packageNode.getStartPosition();
			final boolean hasJavaDoc = packageNode.getJavadoc() != null;
			hasCopyrights = commentBeforePackage || hasJavaDoc;
		}
		return hasCopyrights;
	}

	/**
	 * Returns list of {@link Comment}
	 *
	 * @param compilationUnit
	 *            compilation unit to be analyzed
	 * @return lists of comments
	 */
	@SuppressWarnings("unchecked")
	private List<Comment> getCommentList(final CompilationUnit compilationUnit) {
		return compilationUnit.getCommentList();
	}

	/**
	 * Gets compilation unit's source
	 *
	 * @param unit
	 *            affected compilation unit
	 * @param comment
	 *            comment to be replaced; set null if comment is not present
	 * @return new compilation unit's source
	 */
	private String getNewUnitSource(final ICompilationUnit unit, final Comment comment) {
		String source = null;
		try {
			source = unit.getSource();
			if (comment != null) {
				final int endOfComment = comment.getLength() + comment.getStartPosition();
				source = source.replace(source.substring(0, endOfComment), getCopyrightText());
			}
		} catch (final JavaModelException e) {
			ConsoleUtils.printError(e.getMessage());
		}
		return source;
	}

	/**
	 * Rewrites compilation unit with new source
	 *
	 * @param unit
	 *            compilation unit to be rewritten
	 * @param source
	 *            new source of compilation unit
	 */
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
