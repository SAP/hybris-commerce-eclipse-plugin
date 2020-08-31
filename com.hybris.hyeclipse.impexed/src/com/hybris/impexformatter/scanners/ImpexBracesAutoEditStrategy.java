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
package com.hybris.impexformatter.scanners;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public class ImpexBracesAutoEditStrategy implements IAutoEditStrategy {

	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		
		if (command.text.equalsIgnoreCase("\"")) {
			command.text = "\"\"";
			configureCommand(command);
		}
		else if (command.text.equalsIgnoreCase("'")) {
			command.text = "''";
			configureCommand(command);
		}
		else if (command.text.equalsIgnoreCase("[")) {
			command.text = "[]";
			configureCommand(command);
		}
		else if (command.text.equalsIgnoreCase("(")) {
			command.text = "()";
			configureCommand(command);
		}

	}

	private void configureCommand(DocumentCommand command) {
		// puts the caret between both the quotes

		command.caretOffset = command.offset + 1;
		command.shiftsCaret = false;

	}

}
