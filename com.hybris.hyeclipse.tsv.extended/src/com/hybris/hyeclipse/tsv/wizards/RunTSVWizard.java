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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.hybris.hyeclipse.tsv.editors.TSVResultsInput;
import com.hybris.hyeclipse.tsv.editors.TSVResultsStorage;
//import com.hybris.ps.tsv.main.CmdLineOptions;
//import com.hybris.ps.tsv.main.TSVMain;
//import com.hybris.ps.tsv.output.OutputFormat;
//import com.hybris.ps.tsv.results.IResult;
//import com.hybris.ps.tsv.rules.IRuleSet;

public class RunTSVWizard extends Wizard implements INewWizard {
	private RunTSVWizardPage page;
	private ISelection selection;
	private String resultsString;
	
	private static final String TSV_SPRING_CONFIG = "tsv-spring-config.xml";
    private static final String TSV_MAIN_BEAN = "tsvMain";
    private static final String TSV_OPTIONS_BEAN = "cmdLineOptions";

	public RunTSVWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		page = new RunTSVWizardPage(selection);
		addPage(page);
	}

	public boolean performFinish() {
		
		final File scanDir = page.getScanDirectory();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(scanDir, monitor);
				}
				catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					monitor.done();
				}
			}
		};
		
		try {
			getContainer().run(true, false, op);
		}
		catch (InterruptedException e) {
			return false;
		}
		catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method...
	 */
	private void doFinish(File scanDir, IProgressMonitor monitor) throws CoreException {
		
		monitor.beginTask("Creating analysis file", 3);
		monitor.worked(1);
		
		try {
			runTSVAnalysis(scanDir);
			monitor.worked(1);
			
			monitor.setTaskName("Opening results file...");
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					
					try {
						IStorage storage = new TSVResultsStorage(getResultsString(), new Path("garbage"));
						IStorageEditorInput input = new TSVResultsInput(storage, "TSV Analysis");
						IDE.openEditor(page, input, "com.hybris.hyeclipse.tsv.editors.TSVEditor", true);
					}
					catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			});
			monitor.worked(1);
		}
		catch (RuntimeException re) {
			System.out.println(re.getMessage());
		}
		
	}
	
	private void runTSVAnalysis(File scanDir) {
		
//		ApplicationContext appContext = new ClassPathXmlApplicationContext(new String[]{ TSV_SPRING_CONFIG });
//		TSVMain tsvMain = (TSVMain) appContext.getBean(TSV_MAIN_BEAN);
//		CmdLineOptions options = (CmdLineOptions) appContext.getBean(TSV_OPTIONS_BEAN);
//		options.setErrorsOnly(true);
//		
//		List<File> inputFiles = new ArrayList<File>();
//		inputFiles.add(scanDir);
//		try {
//			List<File> files = tsvMain.getFileService().locateFiles(inputFiles);
//			IRuleSet ruleSet = tsvMain.getRuleService().loadDefaultRules();
//			
//			for (File file : files) {
//				tsvMain.getTestExecutionService().execute(file,  ruleSet);
//	        }
//			
//			List<IResult> results = tsvMain.getResultProvider().getResults();
//			
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			tsvMain.getOutputGenerator().generate(baos, OutputFormat.XML, results);
//			byte[] bytes = baos.toByteArray();
//	        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//	        setResultsString(fromStream(bais));
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
		
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
	
	public String getResultsString() {
		return resultsString;
	}

	public void setResultsString(String resultsString) {
		this.resultsString = resultsString;
	}
    
    public static String fromStream(InputStream in) throws IOException
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    StringBuilder out = new StringBuilder();
	    String newLine = System.getProperty("line.separator");
	    String line;
	    while ((line = reader.readLine()) != null) {
	        out.append(line);
	        out.append(newLine);
	    }
	    return out.toString();
	}
	
}
