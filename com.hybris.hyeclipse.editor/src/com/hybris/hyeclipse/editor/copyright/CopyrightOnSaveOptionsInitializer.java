package com.hybris.hyeclipse.editor.copyright;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

import com.hybris.hyeclipse.editor.copyright.constant.CopyrightConstants;

public class CopyrightOnSaveOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(final CleanUpOptions options) {
		options.setOption(CopyrightConstants.CLEANUP_UPDATE_COPYRIGHTS, CleanUpOptions.FALSE);
		options.setOption(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS, CleanUpOptions.FALSE);
	}

}
