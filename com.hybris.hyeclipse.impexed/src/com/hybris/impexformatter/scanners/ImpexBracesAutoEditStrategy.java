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
