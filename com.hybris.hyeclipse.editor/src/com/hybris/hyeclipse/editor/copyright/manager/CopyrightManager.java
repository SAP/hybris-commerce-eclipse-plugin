package com.hybris.hyeclipse.editor.copyright.manager;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class CopyrightManager {

	public String getCopyrightText() {
		// TODO temporary solution - should be taken from preferences
		return "/* temporary copyright\n" + "*  text\n" + "*/\n";
	}

	public void addCopyrightsHeader(final CompilationUnit compilationUnit) {
		final ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());
		final ListRewrite listRewrite = rewriter.getListRewrite(compilationUnit.getPackage(),
				PackageDeclaration.ANNOTATIONS_PROPERTY);
		final Statement placeHolder = (Statement) rewriter.createStringPlaceholder(getCopyrightText(),
				ASTNode.EMPTY_STATEMENT);
		listRewrite.insertFirst(placeHolder, null);
		try {
			final TextEdit edits = rewriter.rewriteAST();
			final ICompilationUnit iCompilationUnit = (ICompilationUnit) compilationUnit.getJavaElement();
			final Document document = new Document(iCompilationUnit.getSource());
			edits.apply(document);
			iCompilationUnit.getBuffer().setContents(document.get());
		} catch (final MalformedTreeException | JavaModelException | BadLocationException e) {
			// TODO replace with consoleUtils from Tomek's plugin
			e.printStackTrace();
		}
	}

	public void replaceCopyrightsHeader(final CompilationUnit compilationUnit) {
		@SuppressWarnings("unchecked")
		final List<Comment> comments = compilationUnit.getCommentList();
		final Comment copyrightComment = comments.stream().filter(comment -> comment.getStartPosition() == 0).findAny()
				.orElse(null);
		if (copyrightComment != null) {
			final int endOfComment = copyrightComment.getLength();
			final ICompilationUnit unit = (ICompilationUnit) compilationUnit.getJavaElement();
			try {
				final ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());
				final TextEdit edits = rewriter.rewriteAST();
				final String source = unit.getSource();
				final Document document = new Document(
						source.replace(source.substring(0, endOfComment + 1), getCopyrightText()));
				edits.apply(document);

				unit.getBuffer().setContents(document.get());
			} catch (final JavaModelException | MalformedTreeException | BadLocationException e) {
				// TODO replace with consoleUtils from Tomek's plugin
				e.printStackTrace();
			}
		}
	}

}
