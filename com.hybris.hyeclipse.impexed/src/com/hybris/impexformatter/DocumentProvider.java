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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
//import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

//import com.hybris.impexformatter.scanners.ImpexPartitionScanner;

public class DocumentProvider extends FileDocumentProvider {

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		
		IDocument document = super.createDocument(element);
		//IDocumentPartitioner partitioner = createDocumentPartitioner();
		IDocumentPartitioner partitioner = new ImpexDocumentPartitioner();
		
		if ((document instanceof IDocumentExtension3)) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			extension3.setDocumentPartitioner(Activator.IMPEX_PARTITIONING, partitioner);
		}
		else {
			document.setDocumentPartitioner(partitioner);
		}
		
		partitioner.connect(document);
		return document;
	}

	//private static IDocumentPartitioner createDocumentPartitioner() {
		
		//return new FastPartitioner(new ImpexPartitionScanner(), ImpexPartitionScanner.PARTITIONS);
	//}
}
