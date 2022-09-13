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
package com.hybris.hyeclipse.tsv.editors;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsv.model.TSVResult;
import com.hybris.hyeclipse.tsv.model.TSVResults;

/**
 * This multi-page editor has 2 pages:
 * <ul>
 * <li>page 0 contains a nested structured text editor.
 * <li>page 1 shows a TreeViewer
 * </ul>
 */
public class TSVEditor extends MultiPageEditorPart implements IResourceChangeListener{

	/** The text editor used in page 0. */
	private TextEditor editor;
	
	private JAXBContext jaxbContext;
	
	public TSVEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	void createPage0() {
		try {
			editor = new StructuredTextEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		}
		catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}
	}
	
	void createPage1() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);
		
		Tree tree = new Tree(composite, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setWidth(250);
		column1.setResizable(true);
		TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
		column2.setWidth(40);
		column2.setResizable(true);
		TreeColumn column3 = new TreeColumn(tree, SWT.LEFT);
		column3.setWidth(250);
		column3.setResizable(true);
		TreeColumn column4 = new TreeColumn(tree, SWT.LEFT);
		column4.setWidth(600);
		column4.setResizable(true);
		
		TreeViewer resultsViewer = new TreeViewer(tree);
		resultsViewer.setLabelProvider(new TSVResultsLabelProvider());
		resultsViewer.setContentProvider(new TSVResultsContentProvider(resultMap));
		resultsViewer.setInput(createModel());
		
		resultsViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				
				IStructuredSelection structuredSelection = (IStructuredSelection) event.getSelection();
		        if (structuredSelection.isEmpty()) return;

		        Object selection = structuredSelection.getFirstElement();
		        
		        if (selection instanceof TSVResult) {
		        	String fileName = ((TSVResult) selection).getFilename();
		        	int lineNumber = ((TSVResult) selection).getLineNumber();
		        	String extensionName = fileName.replaceAll("-items.xml", "");
		        		
		        	IProject extension = ResourcesPlugin.getWorkspace().getRoot().getProject(extensionName);
		        	IFile itemsxml = extension.getFile("resources/" + fileName);
		        	if (itemsxml.exists()) {
			        	IMarker marker;
			        	try {
							marker = itemsxml.createMarker(IMarker.TEXT);
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put(IMarker.LINE_NUMBER, lineNumber);
							marker.setAttributes(map);
							IDE.openEditor(getSite().getPage(), marker);
							marker.delete();
						}
			        	catch (CoreException e) {
							e.printStackTrace();
						}
		        	}
		        	else {
		        		MessageBox dialog = new MessageBox(getContainer().getShell(), SWT.ICON_WARNING | SWT.OK);
		        		dialog.setText("Extension not found");
		        		dialog.setMessage("The extension " + extensionName + " was not found in the workspace. Please import it and try again.");
		        		dialog.open();
		        	}
		        }
			}
			
		});

		int index = addPage(composite);
		setPageText(index, "Results");
	}
	
	HashMap<String, List<TSVResult>> resultMap = new HashMap<String, List<TSVResult>>();
	
	private Object createModel() {
		IEditorInput editorInput = editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			String reportFile = ((IFileEditorInput)editorInput).getFile().getLocation().toOSString();
			File file = new File(reportFile);
			if (file.exists() && file.length() > 0) {
				try {
					Unmarshaller jaxbUnmarshaller = getJaxbContext().createUnmarshaller();
					TSVResults results = (TSVResults) jaxbUnmarshaller.unmarshal(file);
					resultMap.putAll(parseResults(results, resultMap));
					return resultMap;
				}
				catch (JAXBException e) {
					Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "error while reading XML file: " + reportFile, e));
					e.printStackTrace();
				}
			}
		}
		else if (editorInput instanceof IStorageEditorInput) {
			InputStream resultsStream;
			try {
				IStorage tsvStorage = (IStorage) ((IStorageEditorInput)editorInput).getStorage();
				resultsStream = tsvStorage.getContents();
				
				Unmarshaller jaxbUnmarshaller = getJaxbContext().createUnmarshaller();
				TSVResults results = (TSVResults) jaxbUnmarshaller.unmarshal(resultsStream);
				resultMap.putAll(parseResults(results, resultMap));
				return resultMap;
			}
			catch (CoreException|JAXBException e) {
				Activator.getDefault().getLog().log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "error while reading XML content", e));
				e.printStackTrace();
			}
		}
		return null;
	}

	private Map<String,List<TSVResult>> parseResults(TSVResults results, Map<String,List<TSVResult>> resultMap) {
		if (results != null) {
			Set<TSVResult> resultSet = results.getResults();
			if (resultSet != null && resultSet.isEmpty() == false) {
				for (TSVResult result : resultSet) {
					String itemsFile = result.getFilename();
					if (resultMap.containsKey(itemsFile)) {
						List<TSVResult> existingResultList = resultMap.get(itemsFile);
						existingResultList.add(result);
					}
					else {
						List<TSVResult> emptyResultList = new ArrayList<TSVResult>();
						emptyResultList.add(result);
						resultMap.put(itemsFile, emptyResultList);
					}
				}
			}
		}
		return resultMap;
	}

	protected void createPages() {
		createPage0();
		createPage1();
		setActivePage(1);
	}
	
	/**
	 * The <code>MultiPageEditorPart</code> implementation of this 
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	
	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {}
	
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	public void doSaveAs() {}
	
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput) && !(editorInput instanceof IStorageEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput or IStorageEditorInput");
		super.init(site, editorInput);
	}
	
	/* (non-Javadoc)
	 * Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event){}
	
	public JAXBContext getJaxbContext() throws JAXBException {
		if (jaxbContext == null) {
			jaxbContext = JAXBContext.newInstance(TSVResults.class);
		}
		return jaxbContext;
	}
	
	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}
	
}
