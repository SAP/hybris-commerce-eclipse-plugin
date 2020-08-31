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
package com.hybris.hyeclipse.editor.copyright.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import com.hybris.hyeclipse.editor.copyright.manager.CopyrightManager;

/**
 * Implementation of {@link ICleanUpFix} handling Copyright fixes
 */
public class CopyrightFix implements ICleanUpFix {

	private final static CopyrightManager copyrightManager = new CopyrightManager();
	private final CompilationUnitChange change;

	protected CopyrightFix(final CompilationUnitChange change) {
		this.change = change;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CompilationUnitChange createChange(final IProgressMonitor monitor) throws CoreException {
		return change;
	}

	/**
	 * Creates {@link CopyrightFix} according to preferences
	 *
	 * @param compilationUnit
	 *            compilation unit for which fix is applied
	 * @param enabled
	 *            information if copyright fix is enabled
	 * @param override
	 *            information if copyrights should be overriden
	 * @return copyright fix; null if fix was not applied
	 */
	public static CopyrightFix createCleanUp(final CompilationUnit compilationUnit, final boolean enabled,
			final boolean override) {
		CopyrightFix fix = null;
		if (enabled) {
			if (!copyrightManager.hasCopyrightsComment(compilationUnit)) {
				fix = new CopyrightFix(copyrightManager.addCopyrightsHeader(compilationUnit));
			} else if (override) {
				fix = new CopyrightFix(copyrightManager.replaceCopyrightsHeader(compilationUnit));
			}
		}
		return fix;
	}

}
