package com.sap.cc.lsp.impex;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.springframework.stereotype.Service;

import com.sap.cc.lsp.util.ConMap;
import com.sap.cc.lsp.util.DocumentModel;

@Service
public class TextDocumentService implements org.eclipse.lsp4j.services.TextDocumentService {
	
	private final Map<String, DocumentModel> docs = Collections.synchronizedMap(new HashMap<>());

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		return null;
	}
	
	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}
	
	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.supplyAsync(() -> {
			DocumentModel doc = docs.get(params.getTextDocument().getUri());
			Hover res = new Hover();
			res.setContents(doc.getResolvedRoutes().stream()
					.filter(route -> route.getLine() == params.getPosition().getLine())
					.map(route -> route.getName())
					.map(ConMap.INSTANCE.type::get)
					.map(this::getHoverContent)
					.collect(Collectors.toList()));
			return res;
		});
	}
	
	private Either<String, MarkedString> getHoverContent(String type) {
		return Either.forLeft(type);
	}
	
}
