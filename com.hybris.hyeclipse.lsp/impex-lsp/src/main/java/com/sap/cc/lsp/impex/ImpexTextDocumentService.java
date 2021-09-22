//package com.sap.cc.lsp.impex;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
//import org.eclipse.lsp4j.CompletionItem;
//import org.eclipse.lsp4j.CompletionList;
//import org.eclipse.lsp4j.CompletionParams;
//import org.eclipse.lsp4j.DidChangeTextDocumentParams;
//import org.eclipse.lsp4j.DidCloseTextDocumentParams;
//import org.eclipse.lsp4j.DidOpenTextDocumentParams;
//import org.eclipse.lsp4j.DidSaveTextDocumentParams;
//import org.eclipse.lsp4j.Hover;
//import org.eclipse.lsp4j.HoverParams;
//import org.eclipse.lsp4j.MarkedString;
//import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
//import org.eclipse.lsp4j.jsonrpc.messages.Either;
//import org.eclipse.lsp4j.services.TextDocumentService;
//import org.springframework.stereotype.Service;
//
//import com.sap.cc.lsp.util.ConMap;
//import com.sap.cc.lsp.util.DocumentModel;
//
//@Service
//public class ImpexTextDocumentService implements TextDocumentService {
//	
//	private final Map<String, DocumentModel> docs = Collections.synchronizedMap(new HashMap<>());
//
//	@Override
//	public void didOpen(DidOpenTextDocumentParams params) {
//		updateDocument(params.getTextDocument().getUri());
//		
//	}
//
//	@Override
//	public void didChange(DidChangeTextDocumentParams params) {
//		updateDocument(params.getTextDocument().getUri());
//		
//	}
//
//
//	@Override
//	public void didClose(DidCloseTextDocumentParams params) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void didSave(DidSaveTextDocumentParams params) {
//		// TODO Auto-generated method stub
//	}
//	
//	private void updateDocument(String uri) {
//		 findServer
//		   .byPath(uri).forEach(
//		     server -> {
//		      URI uri = languageServerPathTransformer.toFsURI(server.getId(), wsPath);
//		      DidOpenTextDocumentParams clonedOpenTextDocumentParams =
//		        lsParamsCloner.clone(openTextDocumentParams);
//		      clonedOpenTextDocumentParams.getTextDocument().setUri(uri.toString());
//		      server.getTextDocumentService().didOpen(clonedOpenTextDocumentParams);
//		     });
//		
//	}
//	
//	
//	@Override
//	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
//	    return CompletableFutures.computeAsync( checker -> {
//	        var src = this.src.get();
//	        var currentLineIndex = position.getPosition().getLine();
//	        if ( src.size() <= currentLineIndex ) {  //The last new line in the file is not in the list
//	            return Either.forLeft( List.of() );
//	        }
//	        var currentRow = src.get( currentLineIndex );
//	        var currentRowString = currentRow.stream().collect( joining( "," ) );
//	        var currentRowBeforeCursor = currentRowString
//	                //The source may not have been updated due to synchronization timing issues
//	                .substring( 0, Math.min( currentRowString.length(), position.getPosition().getCharacter() ) );
//	        var currentColumn = (int) currentRowBeforeCursor
//	                .chars()
//	                .filter( c -> c == ',' )
//	                .count();
//	        var wordsInSameColumn = src.stream()
//	                                   .filter( l -> l.size() > currentColumn )
//	                                   .map( l -> l.get( currentColumn ) )
//	                                   .filter( s -> !s.isEmpty() )
//	                                   .distinct()
//	                                   .collect( toList() );
//	        logger.debug( "{}", wordsInSameColumn );
//	        var response = wordsInSameColumn.stream()
//	                                        .map( CompletionItem::new )
//	                                        .collect( toList() );
//
//	        return Either.forLeft( response );
//	    } );
//	}
//	
//	@Override
//	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
//		return null;
//	}
//	
//	@Override
//	public CompletableFuture<Hover> hover(HoverParams params) {
//		return CompletableFuture.supplyAsync(() -> {
//			DocumentModel doc = docs.get(params.getTextDocument().getUri());
//			Hover res = new Hover();
//			res.setContents(doc.getResolvedRoutes().stream()
//					.filter(route -> route.getLine() == params.getPosition().getLine())
//					.map(route -> route.getName())
//					.map(ConMap.INSTANCE.type::get)
//					.map(this::getHoverContent)
//					.collect(Collectors.toList()));
//			return res;
//		});
//	}
//	
//	private Either<String, MarkedString> getHoverContent(String type) {
//		return Either.forLeft(type);
//	}
//	
//}
