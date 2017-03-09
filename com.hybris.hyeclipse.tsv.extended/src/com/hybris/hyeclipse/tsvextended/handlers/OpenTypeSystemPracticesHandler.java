package com.hybris.hyeclipse.tsvextended.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.hybris.hyeclipse.tsv.Activator;

public class OpenTypeSystemPracticesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		try {
			String url = getURL();
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (PartInitException | MalformedURLException e) {
			Activator.logError("Could not open link ", e);
		}
		return null;
	}
	
	protected String getURL() {
		return "https://wiki.hybris.com/display/hybrisALF/Type+System";
	}

}
