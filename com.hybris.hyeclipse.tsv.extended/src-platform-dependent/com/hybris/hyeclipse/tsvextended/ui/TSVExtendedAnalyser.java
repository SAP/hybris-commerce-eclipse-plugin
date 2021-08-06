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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.hybris.hyeclipse.platform.Platform;
import com.hybris.hyeclipse.tsv.Activator;
import com.hybris.hyeclipse.tsvextended.utils.ExtensionHolder;
import com.hybris.ps.tsv.extended.service.PlatformService;
import com.hybris.ps.tsv.extended.utils.ClassPathUtils;
import com.hybris.ps.tsv.output.OutputFormat;
import com.hybris.ps.tsv.results.IResult;
import com.hybris.ps.tsv.results.IResultProvider;
import com.hybris.ps.tsv.rules.IRuleSet;
import com.hybris.ps.tsv.services.IOutputGenerator;
import com.hybris.ps.tsv.services.IRuleService;
import com.hybris.ps.tsv.services.ITestExecutionService;

import de.hybris.bootstrap.config.ExtensionInfo;

public class TSVExtendedAnalyser {
	
	private Set<ExtensionHolder> allScannableExtensions;
	private Set<ExtensionHolder> allExtensionsToScan = new HashSet<ExtensionHolder>();
	private Set<IExtensionListViewer> changeListeners = new HashSet<IExtensionListViewer>();
	private Shell shell;
	private String resultsString;
	
	private PlatformService platformService;
	private ApplicationContext applicationContext;
	
	public static final String TSV_SPRING_CONFIG = File.separator + "tsv-spring-config.xml";
    public static final String TSV_EXTENDED_SPRING_CONFIG = File.separator + "tsv-extended-spring-config.xml";
    public static final String TSV_EXTENDED_HYECLIPSE_SPRING_CONFIG = File.separator + "tsv-extended-hyeclipse-spring-config.xml";
    public static final String PLATFORM_SERVICE_BEAN = "platformService";
    public static final String RULE_SERVICE_BEAN = "ruleService";
    public static final String RESULTS_BEAN = "resultProvider";
    public static final String TEST_EXECUTION_SERVICE_BEAN = "testExecutionService";
    public static final String OUTPUT_GENERATOR_BEAN = "outputGenerator";
	
	public TSVExtendedAnalyser(Composite composite) {
		super();
		this.shell = composite.getShell();
		this.initData(shell);
	}

	private void initData(Shell shell) {
		
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		IRunnableWithProgress op = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Loading extension info", 10);
					final File platformDir = Platform.holder.getCurrent().getPlatformHomeFile();
					if (platformDir != null) {
						monitor.worked(1);
						final File ybootstrapJar = new File(platformDir.getAbsolutePath() + File.separator + "bootstrap" + File.separator + "bin" + File.separator + "ybootstrap.jar");
				        if (ybootstrapJar.exists())
				        {
							URLClassLoader urlClassLoader = null;
							try {
								urlClassLoader = new URLClassLoader(new URL[]{ybootstrapJar.toURI().toURL()});
							}
							catch (MalformedURLException e) {
								e.printStackTrace();
							}
							Thread.currentThread().setContextClassLoader(urlClassLoader);
				            ClassPathUtils.urlClassLoader = urlClassLoader;
				        }
				        monitor.worked(1);
						// load application context
				        applicationContext = new FileSystemXmlApplicationContext(TSV_SPRING_CONFIG, TSV_EXTENDED_SPRING_CONFIG, TSV_EXTENDED_HYECLIPSE_SPRING_CONFIG) {

				            @Override
				            public Resource getResource(String location) {
				                return new ClassPathResource(location, TSVExtendedAnalyser.class);
				            }

				            @Override
				            public ClassLoader getClassLoader() {
				                return TSVExtendedAnalyser.class.getClassLoader();
				            }
				        };
				        monitor.worked(1);
				        
				        if (TSVExtendedAnalyser.this.platformService == null) {
				        	// init platform service
				        	PlatformService platformService = (PlatformService) applicationContext.getBean(PLATFORM_SERVICE_BEAN);
				        	platformService.init(new File(platformDir.getAbsolutePath()));
				        	TSVExtendedAnalyser.this.platformService = platformService;
				        }
						
						monitor.worked(6);
					}
				}
				finally {
					monitor.done();
				}
			}
			
		};
		
		try {
			new ProgressMonitorDialog(shell).run(true, false, op);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
		
	}
	
	/**
	 * Return the collection of extensions
	 */
	public Set<ExtensionHolder> getAllScannableExtensions() {
		
		if (allScannableExtensions == null) {
			PlatformService platformService = this.platformService;
			if (platformService != null) {
				List<ExtensionInfo> projectExtensions = platformService.getProjectExtensions();
				List<ExtensionInfo> templateExtensions = platformService.getTemplateExtensions();
				int scannableSize = projectExtensions.size() + templateExtensions.size();
				Set<ExtensionHolder> extensionsToScan = new HashSet<ExtensionHolder>(scannableSize);
				List<ExtensionInfo> combinedList = new ArrayList<ExtensionInfo>(projectExtensions);
				combinedList.addAll(templateExtensions);
				
				for (ExtensionInfo extensionInfo : combinedList) {
					ExtensionHolder extensionHolder = new ExtensionHolder(extensionInfo);
					extensionHolder.setName(extensionInfo.getName());
					if (projectExtensions.contains(extensionInfo)) {
						extensionHolder.setSelected(true);
						allExtensionsToScan.add(extensionHolder);
					}
					extensionsToScan.add(extensionHolder);
				}
				
				allScannableExtensions = extensionsToScan;
			}
		}
		return allScannableExtensions;
	}
	
	public List<ExtensionInfo> getAllExtensionsToScan() {
		//turn list of ExtensionHolder into list of ExtensionInfo
		if (allExtensionsToScan.isEmpty() == false) {
			List<ExtensionInfo> extensionInfosToScan = new ArrayList<ExtensionInfo>(allExtensionsToScan.size());
			for (ExtensionHolder extensionHolder : allExtensionsToScan) {
				extensionInfosToScan.add(extensionHolder.getExtensionInfo());
			}
			return extensionInfosToScan;
		}
		
		return null;
	}
	
	public void extensionChanged(final ExtensionHolder extension) {
		if (extension.isSelected()) {
			allExtensionsToScan.add(extension);
		}
		else {
			allExtensionsToScan.remove(extension);
		}
		
		Iterator<IExtensionListViewer> iterator = changeListeners.iterator();
		while (iterator.hasNext()) {
			((IExtensionListViewer) iterator.next()).updateExtensionList(extension);
		}
		
	}
	
	public void removeChangeListener(IExtensionListViewer viewer) {
		changeListeners.remove(viewer);
	}
	
	public void addChangeListener(IExtensionListViewer viewer) {
		changeListeners.add(viewer);
	}
	
	protected void runTSVAnalysis() {
		
		final ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			
			//Do actual work here
			IRunnableWithProgress op = new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Analysing extensions...", 10);
			        final IRuleService ruleService = (IRuleService) applicationContext.getBean(RULE_SERVICE_BEAN);
			        final IRuleSet ruleSet = ruleService.loadRulesFromStream(this.getClass().getResourceAsStream("/extended-ruleset.xml"));
			        final ITestExecutionService testExecutionService = (ITestExecutionService) applicationContext.getBean(TEST_EXECUTION_SERVICE_BEAN);
			        final IResultProvider resultProvider = (IResultProvider) applicationContext.getBean(RESULTS_BEAN);
			        final IOutputGenerator outputGenerator = (IOutputGenerator) applicationContext.getBean(OUTPUT_GENERATOR_BEAN);
			        
			        try {
			            final List<File> itemsXmlFiles = new LinkedList<File>();
			            processItemsXmlForExtensions(itemsXmlFiles, getAllExtensionsToScan());
			            resultProvider.resetResults();
			            
			            // execute testExecutionService for each -items.xml
			            for (File file : itemsXmlFiles) {
			            	testExecutionService.execute(file,  ruleSet);
			            }
			            
			            List<IResult> results = resultProvider.getResults();
			            
			            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			            outputGenerator.generate(baos, OutputFormat.XML, results);
			            byte[] bytes = baos.toByteArray();
			            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			            setResultsString(fromStream(bais));
			            
			        }
			        catch (Exception e) {
			        	e.printStackTrace();
			        }
					monitor.worked(5);
					monitor.done();
				}
			};
			
			try {
				new ProgressMonitorDialog(this.shell).run(true, false, op);
			}
			catch (InvocationTargetException e) {
				Activator.logError("thrown exception", e);
			}
			catch (InterruptedException e) {
				Activator.logError("interrupted thread", e);
				Thread.currentThread().interrupted();
			}
			
		}
		finally {
			Thread.currentThread().setContextClassLoader(oldTccl);
		}
		
	}
	
	private void processItemsXmlForExtensions(final List<File> itemsXmlFiles, final List<ExtensionInfo> extensions) {
		if (extensions != null && extensions.isEmpty() == false) {
			for (ExtensionInfo info : extensions) {
				if (info.getItemsXML() != null && info.getItemsXML().exists()) {
					itemsXmlFiles.add(info.getItemsXML());
				}
			}
		}
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
