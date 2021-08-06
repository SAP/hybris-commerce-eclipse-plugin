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
package com.hybris.impexformatter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

public class ImpexDocumentPartitioner implements IDocumentPartitioner {
	
	public static final String IMPEX_DATA = "__y_impex_data";
	public static final String IMPEX_COMMENT = "__y_impex_comment";
	public static final String IMPEX_INSTRUCTION = "__y_impex_instruction";
	public static final String IMPEX_HEADER = "__y_impex_header";
	private IDocument fDocument;
	
	public ImpexDocumentPartitioner() {
		// intended to be empty
	}
	
	@Override
	public void connect(IDocument document) {
		Assert.isNotNull(document);
		fDocument = document;
	}
	
	@Override
	public String getContentType(int offset) {
		
		try {
			IRegion lineInfo = fDocument.getLineInformationOfOffset(offset);
			String line = fDocument.get(lineInfo.getOffset(), lineInfo.getLength());
			
			if (line.startsWith("INSERT ") || line.startsWith("INSERT_UPDATE ") || line.startsWith("UPDATE ") || line.startsWith("REMOVE ")) {
				return ImpexDocumentPartitioner.IMPEX_HEADER;
			}
			if (line.startsWith("\"#%") || line.startsWith("#%")) {
				return ImpexDocumentPartitioner.IMPEX_INSTRUCTION;
			}
			if (line.startsWith("#")) {
				return ImpexDocumentPartitioner.IMPEX_COMMENT;
			}
			if (line.startsWith(";")) {
				return ImpexDocumentPartitioner.IMPEX_DATA;
			}
			if (line.startsWith("$")) {
				// need to handle macros, user rights $START_USERRIGHTS
				return null;
			}
		}
		catch (BadLocationException e) {
			Activator.logError("BadLocationException", e);
		}
		
		return IDocument.DEFAULT_CONTENT_TYPE;
	}
	
	private int getLineEndOffset(int line, IDocument document) throws BadLocationException {
		
		int length = document.getLineLength(line);
		int start = document.getLineOffset(line);
		return start + length - 1;
	}
	
	@Override
	public ITypedRegion getPartition(int arg0) {
		/*
		 * Never gets called because this class implements IDocumentPartitioner
		 * rather than IDocumentPartitionerExtension2 which is required for
		 * IDocumentExtension3
		 * Confused ?? Me too...
		 */
		return new TypedRegion(0, fDocument.getLength(), IMPEX_HEADER);
	}

	@Override
	public ITypedRegion[] computePartitioning(int offset, int length) {
		/*
		 * Never gets called because this class implements IDocumentPartitioner
		 * rather than IDocumentPartitionerExtension2 which is required for
		 * IDocumentExtension3
		 * Confused ?? Me too...
		 */
		List<TypedRegion> list = new ArrayList<TypedRegion>();
		try {
			int start;
			int nextOffset;
			boolean isHeader = true;
			int docLength = fDocument.getLength();
			
			if (offset == 0) {
				nextOffset = getLineEndOffset(1, fDocument);
				list.add(new TypedRegion(0, nextOffset + 1, IMPEX_HEADER));
				
				int i = 1;
				while (nextOffset + 1 < docLength) {
					start = nextOffset+ 1;
					if (Character.isDigit(fDocument.getChar(start))) {
						isHeader = true;
					}
					else {
						isHeader = false;
					}
					
					nextOffset = getLineEndOffset(i + 1, fDocument);
					if (isHeader) {
						list.add(new TypedRegion(start, nextOffset - start + 1, IMPEX_INSTRUCTION));
					}
					else {
						list.add(new TypedRegion(start, nextOffset - start + 1, IMPEX_DATA));
					}
					i = i + 1;
				}
			}
			else {
				if (Character.isDigit(fDocument.getChar(offset))) {
					isHeader = true;
				}
				else {
					isHeader = false;
				}
				
				if (isHeader) {
					list.add(new TypedRegion(offset, length, IMPEX_HEADER));
				}
				else {
					list.add(new TypedRegion(offset, length, IMPEX_DATA));
				}
			}
			
		}
		catch (BadLocationException ble) {
			Activator.logError("BadLocationException", ble);
		}
		
		if (list.isEmpty()) {
			list.add(new TypedRegion(offset, length, null));
		}
		
		TypedRegion[] result = new TypedRegion[list.size()];
		list.toArray(result);
		return result;
	}

	@Override
	public void disconnect() {
		// Intentionally left blank
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent arg0) {
		// Intentionally left blank
	}

	@Override
	public boolean documentChanged(DocumentEvent arg0) {
		return false;
	}

	@Override
	public String[] getLegalContentTypes() {
		return new String[] {};
	}

}
