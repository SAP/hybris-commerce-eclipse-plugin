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
package com.hybris.hyeclipse.tsv.wizards;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RunTSVWizardPage extends WizardPage {
	
	private Text scanDirectoryText;
	private Text containerText;
	private Text fileText;

	public RunTSVWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Run TSV analysis");
		setDescription("This wizard runs a TSV analysis and opens the results.");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		
		Label label = new Label(container, SWT.NULL);
		label.setText("&Scan directory:");
		
		scanDirectoryText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		scanDirectoryText.setLayoutData(gd);
		scanDirectoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("  Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFilesystem();
			}
		});
		
		dialogChanged();
		setControl(container);
	}
	
	private void handleBrowseFilesystem() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setText("Select the scan directory");
		String selectedDir = dialog.open();
		if (selectedDir != null && selectedDir.isEmpty() == false) {
			scanDirectoryText.setText(selectedDir);
		}
	}

	/**
	 * Ensures that all fields are set.
	 */
	private void dialogChanged() {
		
		if (getScanDirectoryName().length() == 0) {
			updateStatus("Scan directory must be specified");
			return;
		}
		
		if (getScanDirectory() == null || !getScanDirectory().exists()) {
			updateStatus("Scan directory must exist");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public String getScanDirectoryName() {
		return scanDirectoryText.getText();
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}

	public File getScanDirectory() {
		String scanDirectory = getScanDirectoryName();
		if (scanDirectory != null && scanDirectory.isEmpty() == false) {
			return new File(scanDirectory);
		}
		else {
			return null;
		}
	}
	
}
