package com.hybris.yps.hyeclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.hybris.yps.hyeclipse.extensionmods.ModuleTableViewer;

public class ConfigureExtensionModulesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		
		Shell shell = new Shell();
		shell.setText("Extension Module Configurations");

		
		shell.setSize(500, 400);
		
		Monitor primary = shell.getDisplay().getPrimaryMonitor();
	    Rectangle bounds = primary.getBounds();
	    Rectangle rect = shell.getBounds();
	    
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = bounds.y + (bounds.height - rect.height) / 2;
	    
	    shell.setLocation(x, y);
		
		// Set layout for shell
		GridLayout layout = new GridLayout();
		shell.setLayout(layout);
		
		// Create a composite to hold the children
		Composite composite = new Composite(shell, SWT.NONE);
		final ModuleTableViewer moduleTableViewer = new ModuleTableViewer(composite);
		if (moduleTableViewer.isPlatformFound()) {
			moduleTableViewer.getControl().addDisposeListener(new DisposeListener() {
	
				@Override
				public void widgetDisposed(DisposeEvent e) {
					moduleTableViewer.dispose();
				}
			});
			
			// Ask the shell to display its content
			shell.open();
			moduleTableViewer.run(shell);
		}
		else {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
			dialog.setText("Platform extension not found");
			dialog.setMessage("The platform extension was not found in the workspace. Please import it and try again.");
			dialog.open();
		}
		
		return null;
	}

}
