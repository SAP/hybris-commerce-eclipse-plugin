package com.hybris.yps.hyeclipse.extensionmods;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

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

import com.hybris.yps.hyeclipse.ExtensionHolder;

public class ModuleTableViewer {
	
	public ModuleTableViewer(Composite parent) {
		this.addChildControls(parent);
	}
	
	private Table table;
	private TableViewer tableViewer;
	private Button closeButton;
	private boolean platformFound = false;
	
	public boolean isPlatformFound() {
		return platformFound;
	}

	private void setPlatformFound(boolean platformFound) {
		this.platformFound = platformFound;
	}

	// Create an ExtensionModuleConfigurer and assign it to an instance variable
	private ExtensionModuleConfigurer emc;
	
	// Set the table column property names
	private final String NAME_COLUMN = "name";
	private final String CORE_COLUMN = "coreModule";
	private final String HMC_COLUMN = "hmcModule";
	private final String WEB_COLUMN = "webModule";
	
	// Set column names
	private String[] columnNames = new String[] {
			NAME_COLUMN, 
			CORE_COLUMN,
			WEB_COLUMN,
			HMC_COLUMN
		};
	
	/**
	 * Run and wait for a close event
	 * @param shell Instance of Shell
	 */
	public void run(Shell shell) {
		
		// Add a listener for the close button
		closeButton.addSelectionListener(new SelectionAdapter() {
			// Close the view i.e. dispose of the composite's parent
			public void widgetSelected(SelectionEvent e) {
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
		
		// The input for the table viewer is the instance of ExtensionModuleConfigurer
		emc = new ExtensionModuleConfigurer(composite);
		if (emc.getAllPlatformExtensions() != null) {
			tableViewer.setInput(emc);
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
		
		//Create and configure the "Close" button
		closeButton = new Button(parent, SWT.PUSH | SWT.CENTER);
		closeButton.setText("Close");
		GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_END);
		gridData.widthHint = 80; 
		closeButton.setLayoutData(gridData);
	}

	private void createTable(Composite parent) {
		
		int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | 
				SWT.FULL_SELECTION | SWT.HIDE_SELECTION;

		table = new Table(parent, style);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 3;
		table.setLayoutData(gridData);		
					
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		
		// 1st column with extension name
		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("Extension");
		column.setWidth(200);
		// Add listener to column so tasks are sorted by description when clicked
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//TODO - tableViewer.setSorter(null);
			}
		});
		// 2nd column coremodule checkbox
		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText("core");
		column.setWidth(100);
		// 3rd column webmodule checkbox
		column = new TableColumn(table, SWT.LEFT, 2);
		column.setText("web");
		column.setWidth(100);
		// 4th column hmcmodule checkbox
		column = new TableColumn(table, SWT.LEFT, 3);
		column.setText("hmc");
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
		// Columns 2,3,4 : checkboxes
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new CheckboxCellEditor(table);
		editors[3] = new CheckboxCellEditor(table);
		
		// Assign the cell editors to the viewer 
		tableViewer.setCellEditors(editors);
		// Set the cell modifier for the viewer
		tableViewer.setCellModifier(new ExtensionCellModifier(this));
		// Set the default sorter for the viewer 
		//tableViewer.setSorter(new ExtensionSorter(ExtensionSorter.NAME));
		
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
	public ExtensionModuleConfigurer getEmc() {
		return emc;	
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
		return closeButton;
	}
	
	/**
	 * InnerClass that acts as a proxy for the ExtensionModuleConfigurer 
	 * providing content for the Table. It implements the IExtensionListViewer 
	 * interface since it must register changeListeners with the 
	 * ExtensionModuleConfigurer
	 */
	class ExtensionContentProvider implements IStructuredContentProvider, IExtensionListViewer {

		@Override
		public void dispose() {
			emc.removeChangeListener(this);
		}

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			if (newInput != null) {
				((ExtensionModuleConfigurer) newInput).addChangeListener(this);
			}
			if (oldInput != null) {
				((ExtensionModuleConfigurer) oldInput).removeChangeListener(this);
			}
		}

		@Override
		public void updateExtension(ExtensionHolder extension) {
			tableViewer.update(extension, null);
		}

		@Override
		public Object[] getElements(Object parent) {
			Set<ExtensionHolder> allExtensions = emc.getAllPlatformExtensions();
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

	}

}
