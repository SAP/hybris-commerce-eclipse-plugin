package com.hybris.hyeclipse.platform;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;

final class ClasspathChagedListener implements IElementChangedListener {
	
	final PlatformHolder platformHolder;
	
	ClasspathChagedListener(PlatformHolder platformHolder) {
		this.platformHolder = platformHolder;
	}

	private boolean isClasspathChanged(int flags) {
		return 0 != (flags
				& (IJavaElementDelta.F_CLASSPATH_CHANGED | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED));
	}

	public void visitChildren(IJavaElementDelta delta) {
		for (IJavaElementDelta c : delta.getAffectedChildren()) {
			visit(c);
		}
	}

	private void visit(IJavaElementDelta delta) {
		IJavaElement el = delta.getElement();
		switch (el.getElementType()) {
		case IJavaElement.JAVA_MODEL:
			visitChildren(delta);
			break;
		case IJavaElement.JAVA_PROJECT:
			if (isClasspathChanged(delta.getFlags()) && platformHolder.isCurrentPlatformProject(delta.getElement().getJavaProject())) {
				notifyClasspathChanged((IJavaProject) el);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void elementChanged(final ElementChangedEvent event) {
		visit(event.getDelta());
	}

	private void notifyClasspathChanged(final IJavaProject platformJavaProject) {
		platformHolder.classPathChanged();
	}
}