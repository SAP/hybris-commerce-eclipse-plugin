package com.hybris.hyeclipse.editor.copyright;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

import com.hybris.hyeclipse.editor.copyright.constant.CopyrightConstants;

/**
 * Implementation of {@link ICleanUpOptionsInitializer} sets default clean up
 * options
 */
public class CopyrightOnSaveOptionsInitializer implements ICleanUpOptionsInitializer {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDefaultOptions(final CleanUpOptions options) {
		options.setOption(CopyrightConstants.CLEANUP_ADD_COPYRIGHTS, CleanUpOptions.TRUE);
		options.setOption(CopyrightConstants.CLEANUP_OVERRIDE_COPYRIGHTS, CleanUpOptions.FALSE);
	}

}
