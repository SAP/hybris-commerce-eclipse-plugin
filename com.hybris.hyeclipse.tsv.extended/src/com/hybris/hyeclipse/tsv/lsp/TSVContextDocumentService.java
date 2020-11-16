package com.hybris.hyeclipse.tsv.lsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.hybris.hyeclipse.tsv.lsp.TSVDocumentModel.Route;
import com.hybris.hyeclipse.tsv.lsp.TSVDocumentModel.VariableDefinition;

public class TSVContextDocumentService implements TextDocumentService {
	
	private final TSVLanguageServer languageServer;
	private final Map<String, TSVDocumentModel> docs = Collections.synchronizedMap(new HashMap<>());
	
	public TSVContextDocumentService(TSVLanguageServer _languageServer) {
		this.languageServer = _languageServer;
	}

	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
			TextDocumentPositionParams position) {
		return CompletableFuture.supplyAsync(() -> Either.forLeft(TsvLspMap.INSTANCE.all.stream()
				.map(word -> {
					CompletionItem item = new CompletionItem();
					item.setLabel(word);
					item.setInsertText(word);
					return item;
				}).collect(Collectors.toList())));
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return null;
	}

	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			TSVDocumentModel doc = docs.get(position.getTextDocument().getUri());
			Hover res = new Hover();
			res.setContents(doc.getResolvedRoutes().stream()
				.filter(route -> route.line == position.getPosition().getLine())
				.map(route -> route.name)
				.map(TsvLspMap.INSTANCE.type::get)
				.map(this::getHoverContent)
				.collect(Collectors.toList()));
			return res;
		});
	}
	
	private Either<String, MarkedString> getHoverContent(String type) {
		return Either.forLeft(type);
	}

	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		return null;
	}

	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
		return CompletableFuture.supplyAsync(() -> {
			TSVDocumentModel doc = docs.get(position.getTextDocument().getUri());
			String variable = doc.getVariable(position.getPosition().getLine(), position.getPosition().getCharacter()); 
			if (variable != null) {
				int variableLine = doc.getDefintionLine(variable);
				if (variableLine == -1) {
					return Collections.emptyList();
				}
				Location location = new Location(position.getTextDocument().getUri(), new Range(
					new Position(variableLine, 0),
					new Position(variableLine, variable.length())
					));
				return Collections.singletonList(location);
			}
			return null;
		});
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return CompletableFuture.supplyAsync(() -> {
			TSVDocumentModel doc = docs.get(params.getTextDocument().getUri());
			String variable = doc.getVariable(params.getPosition().getLine(), params.getPosition().getCharacter()); 
			if (variable != null) {
				return doc.getResolvedRoutes().stream()
					.filter(route -> route.text.contains("${" + variable + "}") || route.text.startsWith(variable + "="))
					.map(route -> new Location(params.getTextDocument().getUri(), new Range(
						new Position(route.line, route.text.indexOf(variable)),
						new Position(route.line, route.text.indexOf(variable) + variable.length())
					)))
					.collect(Collectors.toList());
			}
			String routeName = doc.getResolvedRoutes().stream()
					.filter(route -> route.line == params.getPosition().getLine())
					.collect(Collectors.toList())
					.get(0)
					.name;
			return doc.getResolvedRoutes().stream()
					.filter(route -> route.name.equals(routeName))
					.map(route -> new Location(params.getTextDocument().getUri(), new Range(
							new Position(route.line, 0),
							new Position(route.line, route.text.length()))))
					.collect(Collectors.toList());
		});
	}

	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
		return null;
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		TSVDocumentModel model = docs.get(params.getTextDocument().getUri());
		if(model == null)
			return null;
		
		return CompletableFuture.supplyAsync(() ->
			docs.get(params.getTextDocument().getUri()).getResolvedLines().stream().map(line -> {
				SymbolInformation symbol = new SymbolInformation();
				symbol.setLocation(new Location(params.getTextDocument().getUri(), new Range(
						new Position(line.line, line.charOffset),
						new Position(line.line, line.charOffset + line.text.length()))));
				if (line instanceof VariableDefinition) {
					symbol.setKind(SymbolKind.Variable);
					symbol.setName(((VariableDefinition) line).variableName);
				} else if (line instanceof Route) {
					symbol.setKind(SymbolKind.String);
					symbol.setName(((Route) line).name);
				}
				return Either.<SymbolInformation, DocumentSymbol>forLeft(symbol);
			}).collect(Collectors.toList())
		);
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return null;
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		return null;
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return null;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		TSVDocumentModel model = new TSVDocumentModel(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(),
				model);
		CompletableFuture.runAsync(() ->
			languageServer.client.publishDiagnostics(
				new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))
			)
		);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		TSVDocumentModel model = new TSVDocumentModel(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(),
				model);
		// send notification
		CompletableFuture.runAsync(() ->
			languageServer.client.publishDiagnostics(
				new PublishDiagnosticsParams(params.getTextDocument().getUri(), validate(model))
			)
		);
	}

	private List<Diagnostic> validate(TSVDocumentModel model) {
		List<Diagnostic> res = new ArrayList<>();
		Route previousRoute = null;
		for (Route route : model.getResolvedRoutes()) {
			if (!TsvLspMap.INSTANCE.all.contains(route.name)) {
				Diagnostic diagnostic = new Diagnostic();
				diagnostic.setSeverity(DiagnosticSeverity.Error);
				diagnostic.setMessage("This is not a Session");
				diagnostic.setRange(new Range(
						new Position(route.line, route.charOffset),
						new Position(route.line, route.charOffset + route.text.length())));
				res.add(diagnostic);
			} else if (previousRoute != null && !TsvLspMap.INSTANCE.startsFrom(route.name, previousRoute.name)) {
				Diagnostic diagnostic = new Diagnostic();
				diagnostic.setSeverity(DiagnosticSeverity.Warning);
				diagnostic.setMessage("'" + route.name + "' does not follow '" + previousRoute.name + "'");
				diagnostic.setRange(new Range(
						new Position(route.line, route.charOffset),
						new Position(route.line, route.charOffset + route.text.length())));
				res.add(diagnostic);
			}
			previousRoute = route;
		}
		return res;
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		this.docs.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
	}

}
