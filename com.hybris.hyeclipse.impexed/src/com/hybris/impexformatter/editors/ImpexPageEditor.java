/*******************************************************************************
 * Copyright 2020 SAP
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.hybris.impexformatter.editors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.hybris.impexformatter.Activator;
import com.hybris.impexformatter.actions.Formatter;

/**
 * An example showing how to create a multi-page editor. This example has 3 pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
@Deprecated
public class ImpexPageEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The text editor used in page 0. */
	private ImpexEditor editor;

	/**
	 * Creates a multi-page editor example.
	 */
	public ImpexPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all
	 * nested editors. Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text for page 0's tab, and updates this
	 * multi-page editor's input to correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks that the input is an instance of
	 * <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) editor.getEditorInput()).getFile().getProject()
						                .equals(event.getResource())) {
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	static void formatText(IEditorPart editorPart) {
		if(editorPart instanceof ImpexPageEditor) {
			String unformattedText = ((ImpexPageEditor)editorPart).editor.getDocumentProvider().getDocument(editorPart.getEditorInput()).get();
	
			try (BufferedReader reader = new BufferedReader(new StringReader(unformattedText));
			                StringWriter writer = new StringWriter()) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					while (line.toUpperCase(Locale.ENGLISH).matches(Formatter.HEADER_MODE)) {
						// start of new block
						line = Formatter.formatData(reader, writer, line);
						if (line == null) {
							break;
						}
					}
					if (line != null) {
						writer.append(line);
						writer.write(System.getProperty(Formatter.LINE_SEPARATOR));
					}
				}
	
				((ImpexPageEditor)editorPart).editor.getDocumentProvider().getDocument(editorPart.getEditorInput()).set(writer.toString());
	
			}
			catch (final IOException e) {
				Activator.logError("IOException", e);
			}
		}
	}

	protected void createPages() {
		createPage0();
	}

	void createPage0() {
		try {
			editor = new ImpexEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
			setPartName(editor.getTitle());

		}
		catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}
	}
}
