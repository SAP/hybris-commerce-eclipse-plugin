//package com.sap.cc.lsp.impex;
//
//import java.util.Arrays;
//import java.util.concurrent.CompletableFuture;
//
//import org.eclipse.lsp4j.InitializeParams;
//import org.eclipse.lsp4j.InitializeResult;
//import org.eclipse.lsp4j.MessageParams;
//import org.eclipse.lsp4j.MessageType;
//import org.eclipse.lsp4j.ServerCapabilities;
//import org.eclipse.lsp4j.SignatureHelpOptions;
//import org.eclipse.lsp4j.TextDocumentSyncKind;
//import org.eclipse.lsp4j.services.LanguageClient;
//import org.eclipse.lsp4j.services.LanguageClientAware;
//import org.eclipse.lsp4j.services.TextDocumentService;
//import org.eclipse.lsp4j.services.WorkspaceService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class ImpexLanguageServer implements org.eclipse.lsp4j.services.LanguageServer, LanguageClientAware {
//	
//	private int shutdown = 1;
//	private WorkspaceService workspaceService;
//	private TextDocumentService textDocumentService;
//	private LanguageClient client;
//	
//	@Autowired
//	public ImpexLanguageServer(WorkspaceService workspaceService, TextDocumentService textDocumentService) {
//		super();
//		this.workspaceService = workspaceService;
//		this.textDocumentService = textDocumentService;
//	}
//
//	@Override
//	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
//        final InitializeResult res = new InitializeResult(new ServerCapabilities());
//        res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
//
//        final SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions(Arrays.asList("(", ","));
////        final List<String> commandList = LSCommandExecutorProvidersHolder.getInstance(this.serverContext)
////                .getCommandsList();
////        final ExecuteCommandOptions executeCommandOptions = new ExecuteCommandOptions(commandList);
//        final ImpexCompletionOptions completionOptions = new ImpexCompletionOptions();
//
//        res.getCapabilities().setCompletionProvider(completionOptions);
//        res.getCapabilities().setSignatureHelpProvider(signatureHelpOptions);
//        res.getCapabilities().setHoverProvider(true);
////        res.getCapabilities().setDocumentSymbolProvider(false);
////        res.getCapabilities().setDefinitionProvider(true);
////        res.getCapabilities().setReferencesProvider(true);
////        res.getCapabilities().setCodeActionProvider(true);
////        res.getCapabilities().setExecuteCommandProvider(executeCommandOptions);
////        res.getCapabilities().setDocumentFormattingProvider(true);
////        res.getCapabilities().setDocumentRangeFormattingProvider(true);
////        res.getCapabilities().setRenameProvider(true);
////        res.getCapabilities().setWorkspaceSymbolProvider(false);
////        res.getCapabilities().setImplementationProvider(false);
////        res.getCapabilities().setFoldingRangeProvider(true);
////        res.getCapabilities().setCodeLensProvider(new CodeLensOptions());
//
////        HashMap experimentalClientCapabilities = null;
////        if (params.getCapabilities().getExperimental() != null) {
////            experimentalClientCapabilities = new Gson().fromJson(params.getCapabilities().getExperimental().toString(),
////                                                                 HashMap.class);
////        }
//
////        // Set AST provider and examples provider capabilities
////        HashMap<String, Object> experimentalServerCapabilities = new HashMap<>();
////        experimentalServerCapabilities.put(AST_PROVIDER.getValue(), true);
////        experimentalServerCapabilities.put(EXAMPLES_PROVIDER.getValue(), true);
////        experimentalServerCapabilities.put(API_EDITOR_PROVIDER.getValue(), true);
////
////        if (experimentalClientCapabilities != null) {
////            Object introspectionObj = experimentalClientCapabilities.get(INTROSPECTION.getValue());
////            if (introspectionObj instanceof Boolean && (Boolean) introspectionObj) {
////                int port = ballerinaTraceListener.startListener();
////                experimentalServerCapabilities.put(INTROSPECTION.getValue(), new ProviderOptions(port));
////            }
////        }
////        res.getCapabilities().setExperimental(experimentalServerCapabilities);
////
////
////        TextDocumentClientCapabilities textDocClientCapabilities = params.getCapabilities().getTextDocument();
////        WorkspaceClientCapabilities workspaceClientCapabilities = params.getCapabilities().getWorkspace();
////        LSClientCapabilities capabilities = new LSClientCapabilitiesImpl(textDocClientCapabilities,
////                                                                         workspaceClientCapabilities,
////                                                                         experimentalClientCapabilities);
////        ((BallerinaTextDocumentService) textService).setClientCapabilities(capabilities);
////        ((BallerinaWorkspaceService) workspaceService).setClientCapabilities(capabilities);
//        
//        return CompletableFuture.supplyAsync(() -> res);
//	}
//
//	@Override
//	public CompletableFuture<Object> shutdown() {
//		shutdown = 1;
//		return CompletableFuture.supplyAsync(Object::new);
//	}
//
//	@Override
//	public void exit() {
//		System.exit(shutdown);
//	}
//
//	@Override
//	public TextDocumentService getTextDocumentService() {
//		return this.textDocumentService;
//	}
//
//	@Override
//	public WorkspaceService getWorkspaceService() {
//		return this.workspaceService;
//	}
//	
//	@Override
//	public void connect(LanguageClient remoteProxy) {
//		this.client = remoteProxy;
//		this.client.logMessage(new MessageParams(MessageType.Info, "connected"));
//	}
//
//}
