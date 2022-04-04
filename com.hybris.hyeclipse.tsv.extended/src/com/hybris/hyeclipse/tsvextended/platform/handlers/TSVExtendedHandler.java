///*******************************************************************************
// * Copyright 2020 SAP
// * 
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License.  You may obtain a copy
// * of the License at
// * 
// *   http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// * License for the specific language governing permissions and limitations under
// * the License.
// ******************************************************************************/
//package com.hybris.hyeclipse.tsvextended.platform.handlers;
//
//import org.eclipse.core.commands.AbstractHandler;
//import org.eclipse.core.commands.ExecutionEvent;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.events.DisposeEvent;
//import org.eclipse.swt.events.DisposeListener;
//import org.eclipse.swt.graphics.Rectangle;
//import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.MessageBox;
//import org.eclipse.swt.widgets.Monitor;
//import org.eclipse.swt.widgets.Shell;
//
//import com.hybris.hyeclipse.tsvextended.ui.ModuleTableViewer;
//
//public class TSVExtendedHandler extends AbstractHandler implements Runnable {
//	
//	@Override
//	public Object execute(ExecutionEvent event) {
//		run();
//		return null;
//	}
//	
//	@Override
//	public void run() {
//		
//		Shell shell = new Shell();
//		shell.setText("Extended TSV Analysis");
//		shell.setSize(300, 400);
//		
//		Monitor primary = shell.getDisplay().getPrimaryMonitor();
//	    Rectangle bounds = primary.getBounds();
//	    Rectangle rect = shell.getBounds();
//	    
//	    int x = bounds.x + (bounds.width - rect.width) / 2;
//	    int y = bounds.y + (bounds.height - rect.height) / 2;
//	    
//	    shell.setLocation(x, y);
//		
//		// Set layout for shell
//		GridLayout layout = new GridLayout();
//		shell.setLayout(layout);
//		
//		// Create a composite to hold the children
//		Composite composite = new Composite(shell, SWT.NONE);
//		final ModuleTableViewer moduleTableViewer = new ModuleTableViewer(composite);
//		if (moduleTableViewer.isPlatformFound()) {
//			moduleTableViewer.getControl().addDisposeListener(new DisposeListener() {
//	
//				@Override
//				public void widgetDisposed(DisposeEvent e) {
//					moduleTableViewer.dispose();
//				}
//			});
//			
//			// Ask the shell to display its content
//			shell.open();
//			moduleTableViewer.run(shell);
//		}
//		else {
//			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
//			dialog.setText("Platform extension not found");
//			dialog.setMessage("The platform extension was not found in the workspace. Please import it and try again.");
//			dialog.open();
//		}
//		
//	}
//
//}
