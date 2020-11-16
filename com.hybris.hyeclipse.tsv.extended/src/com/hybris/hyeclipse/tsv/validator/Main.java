package com.hybris.hyeclipse.tsv.validator;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import com.hybris.hyeclipse.tsv.lsp.TSVLanguageServer;

public class Main {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		startServer(System.in, System.out);
	}

	public static void startServer(InputStream in, OutputStream out) throws InterruptedException, ExecutionException {
		//Put the code to start the Language Server here
		TSVLanguageServer server = new TSVLanguageServer();
		Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
		Future<Void> listening = launcher.startListening();
		server.setClient(launcher.getRemoteProxy());
		listening.get();
	}

}