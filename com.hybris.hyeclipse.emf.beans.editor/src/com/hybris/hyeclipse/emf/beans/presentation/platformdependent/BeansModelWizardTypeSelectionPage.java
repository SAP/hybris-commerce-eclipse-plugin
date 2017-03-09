package com.hybris.hyeclipse.emf.beans.presentation.platformdependent;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.hybris.hyeclipse.ytypesystem.Activator;

import de.hybris.bootstrap.typesystem.YComposedType;
import de.hybris.bootstrap.typesystem.YType;

class BeansModelWizardTypeSelectionPage extends WizardPage {
	
	static class YTypeCodePrefixFilter extends ViewerFilter {
		private final String prefixFilter;

		YTypeCodePrefixFilter(String prefixFilter) {
			this.prefixFilter = prefixFilter.trim();
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return element instanceof YType && ((YType) element).getCode().startsWith(prefixFilter);
		}
	}

	private CheckboxTableViewer tableViewer;
	private Text filterText;
	private Collection<YType> selectedTypes;

	BeansModelWizardTypeSelectionPage(String pageName) {
		super(pageName);
		this.selectedTypes = new HashSet<>();
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			layout.verticalSpacing = 12;
			composite.setLayout(layout);

			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.grabExcessVerticalSpace = true;
			data.horizontalAlignment = GridData.FILL;
			composite.setLayoutData(data);
		}
		filterText = new Text(composite, SWT.BORDER);
		filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.NONE);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new YTypeLabelProvider());
		final Collection<? extends YType> allTypes = Activator.getDefault().getTypeSystem().getComposedTypes();
		tableViewer.setInput(allTypes);
		
		final Button selectAllButton = new Button(composite, SWT.CHECK);
		selectAllButton.setText("Select All");
		selectAllButton.setLayoutData(new GridData(SWT.BEGINNING));
		
		//We need to handle check/uncheck manually because CheckboxTableViewer
		//does not maintain selection properly when filters change
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					selectedTypes.add((YComposedType) event.getElement());
				}
				else {
					selectedTypes.remove(event.getElement());
				}
				if (selectedTypes.size() == allTypes.size()) {
					selectAllButton.setSelection(true);
					selectAllButton.setText("Clear All");
				}
				else {
					selectAllButton.setSelection(false);
					selectAllButton.setText("Select All");
				}
			}
		});
		filterText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				tableViewer.setFilters(new ViewerFilter[] {new YTypeCodePrefixFilter(filterText.getText())});
				tableViewer.setCheckedElements(selectedTypes.toArray(new Object[selectedTypes.size()]));
			}
		});
		filterText.setText("");
		
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectAllButton.getSelection()) {
					selectedTypes.addAll(allTypes);
					selectAllButton.setText("Clear All");
				}
				else {
					selectedTypes.clear();
					selectAllButton.setText("Select All");
				}
				tableViewer.setCheckedElements(selectedTypes.toArray(new Object[selectedTypes.size()]));
			}
		});
		
		setPageComplete(true);
		setControl(tableViewer.getControl());
	}
	
	//@SuppressWarnings({ "unchecked", "rawtypes" })
	Collection<? extends YType> getSelectedTypes() {
		return selectedTypes;//(Collection) Arrays.asList(tableViewer.getCheckedElements());
	}

}
