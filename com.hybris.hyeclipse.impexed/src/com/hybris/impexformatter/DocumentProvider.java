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
