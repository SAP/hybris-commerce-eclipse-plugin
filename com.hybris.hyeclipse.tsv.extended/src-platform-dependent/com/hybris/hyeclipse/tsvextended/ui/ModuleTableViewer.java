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
package com.hybris.hyeclipse.tsvextended.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.hybris.hyeclipse.tsv.editors.TSVResultsInput;
import com.hybris.hyeclipse.tsv.editors.TSVResultsStorage;
import com.hybris.hyeclipse.tsvextended.utils.ExtensionHolder;

public class ModuleTableViewer {
	
	public ModuleTableViewer(Composite parent) {
		this.addChildControls(parent);
	}
	
	private Table table;
	private TableViewer tableViewer;
	private Button analyseButton;
	private boolean platformFound = false;
	
	public boolean isPlatformFound() {
		return platformFound;
	}

	private void setPlatformFound(boolean platformFound) {
		this.platformFound = platformFound;
	}

	// Create an ExtensionModuleConfigurer and assign it to an instance variable
	private TSVExtendedAnalyser tsvExtendedAnalyser;
	
	// Set the table column property names
	private static final String NAME_COLUMN = "name";
	private static final String ANALYSE_COLUMN = "analyse";
	
	// Set column names
	private String[] columnNames = new String[] {
			NAME_COLUMN, 
			ANALYSE_COLUMN
		};
	
	/**
	 * Run and wait for a close event
	 * @param shell Instance of Shell
	 */
	public void run(final Shell shell) {
		
		// Add a listener for the analyse button
		analyseButton.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent e) {
				
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						
						try {
							getTSVExtendedAnalyser().runTSVAnalysis();
							IStorage storage = new TSVResultsStorage(getTSVExtendedAnalyser().getResultsString(), new Path("garbage"));
							IStorageEditorInput input = new TSVResultsInput(storage);
							IDE.openEditor(page, input, "com.hybris.hyeclipse.tsv.editors.TSVEditor", true);
						}
						catch (PartInitException e) {
							e.printStackTrace();
						} 
					}
				});
				
				// Close the view i.e. dispose of the composite's parent
				table.getParent().getParent().dispose();
			}
		});
		
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
	}
	
	/**
	 * Release resources
	 */
	public void dispose() {
		
		// Tell the label provider to release its resources
		tableViewer.getLabelProvider().dispose();
	}
	
	/**
	 * Create a new shell, add the widgets, open the shell
	 * @return the shell that was created	 
	 */
	private void addChildControls(Composite composite) {
		
		// Create a composite to hold the children
		GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
		composite.setLayoutData (gridData);
		
		// Set numColumns to 1 for the button 
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 4;
		composite.setLayout (layout);

		// Create the table 
		createTable(composite);
		
		// Create and setup the TableViewer
		createTableViewer();
		
		tableViewer.setContentProvider(new ExtensionContentProvider());
		tableViewer.setLabelProvider(new ExtensionLabelProvider());
		
		// The input for the table viewer is the instance of TSVExtendedAnalyser
		tsvExtendedAnalyser = new TSVExtendedAnalyser(composite);
		if (tsvExtendedAnalyser.getAllScannableExtensions() != null) {
			tableViewer.setInput(tsvExtendedAnalyser);
			setPlatformFound(true);
		}
		// Add the buttons
		createButtons(composite);
	}

	/**
	 * Add the "Close" button
	 * @param parent the parent composite
	 */
	private void createButtons(Composite parent) {	
		
		//Create and configure the "Run Analysis" button
		analyseButton = new Button(parent, SWT.PUSH | SWT.CENTER);
		analyseButton.setText("Run Analysis");
		GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_END);
		gridData.widthHint = 120; 
		analyseButton.setLayoutData(gridData);
	}

	private void createTable(Composite parent) {
		
		int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | 
				SWT.FULL_SELECTION | SWT.HIDE_SELECTION;

		table = new Table(parent, style);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 2;
		table.setLayoutData(gridData);		
					
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		// 1st column with extension name
		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("Extension");
		column.setWidth(200);
		// Add listener to column so extensions are sorted by name when clicked
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//TODO
			}
		});
		// 2nd column analyse checkbox
		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText("Analyse?");
		column.setWidth(100);
	}
	
	private void createTableViewer() {
		
		tableViewer = new TableViewer(table);
		tableViewer.setUseHashlookup(true);
		
		tableViewer.setColumnProperties(columnNames);
		
		// Create the cell editors
		CellEditor[] editors = new CellEditor[columnNames.length];
		
		// Column 1 : nothing
		editors[0] = null;
		// Columns 2 : checkbox
		editors[1] = new CheckboxCellEditor(table);
		
		// Assign the cell editors to the viewer 
		tableViewer.setCellEditors(editors);
		// Set the cell modifier for the viewer
		tableViewer.setCellModifier(new ExtensionCellModifier(this));
	}
	
	/*
	 * Close the window and dispose of resources
	 */
	public void close() {
		Shell shell = table.getShell();

		if (shell != null && !shell.isDisposed())
			shell.dispose();
	}
	
	/**
	 * Return the column names in a collection
	 * 
	 * @return List  containing column names
	 */
	public java.util.List<String> getColumnNames() {
		return Arrays.asList(columnNames);
	}

	/**
	 * @return currently selected item
	 */
	public ISelection getSelection() {
		return tableViewer.getSelection();
	}
	
	/**
	 * Return the ExtensionModuleConfigurer
	 */
	public TSVExtendedAnalyser getTSVExtendedAnalyser() {
		return tsvExtendedAnalyser;	
	}

	/**
	 * Return the parent composite
	 */
	public Control getControl() {
		return table.getParent();
	}

	/**
	 * Return the 'close' Button
	 */
	public Button getCloseButton() {
		return analyseButton;
	}
	
	/**
	 * InnerClass that acts as a proxy for the TSVExtendedAnalyser
	 * providing content for the Table. It implements the IExtensionListViewer 
	 * interface since it must register changeListeners with the 
	 * TSVExtendedAnalyser
	 */
	class ExtensionContentProvider implements IStructuredContentProvider, IExtensionListViewer {

		@Override
		public void dispose() {
			tsvExtendedAnalyser.removeChangeListener(this);
		}

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (newInput != null) {
				((TSVExtendedAnalyser) newInput).addChangeListener(this);
			}
			if (oldInput != null) {
				((TSVExtendedAnalyser) oldInput).removeChangeListener(this);
			}
		}

		@Override
		public Object[] getElements(Object parent) {
			Set<ExtensionHolder> allExtensions = tsvExtendedAnalyser.getAllScannableExtensions();
			if (allExtensions != null) {
				TreeSet<ExtensionHolder> sortedExtensions = new TreeSet<ExtensionHolder>(new Comparator<ExtensionHolder>() {
					@Override
					public int compare(final ExtensionHolder object1, final ExtensionHolder object2) {
						return object1.getName().compareTo(object2.getName());
					}
				});
				sortedExtensions.addAll(allExtensions);
				
				return sortedExtensions.toArray();
			}
			return null;
		}

		@Override
		public void updateExtensionList(ExtensionHolder extension) {
			tableViewer.update(extension, null);
		}

	}

}
