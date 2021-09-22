//package com.sap.cc.lsp.impex;
//
//import java.util.concurrent.Future;
//
//import org.eclipse.lsp4j.jsonrpc.Launcher;
//import org.eclipse.lsp4j.services.LanguageClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.Banner;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//@SpringBootApplication
//public class LSPLauncher implements CommandLineRunner {
//	
//	private final ImpexLanguageServer languageServer;
//	
//	@Autowired
//	public LSPLauncher(final ImpexLanguageServer languageServer) {
//		super();
//		this.languageServer = languageServer;
//	}
//
//	public static void main(String[] args) {
//		SpringApplication app = new SpringApplication(LSPLauncher.class);
//		app.setBannerMode(Banner.Mode.OFF);
//		app.run(args);
//	}
//
//	@Override
//	public void run(String... args) throws Exception {
//		Launcher<LanguageClient> l = org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher(languageServer, System.in, System.out);
//		Future<?> startListening = l.startListening();
//		startListening.get();
//		languageServer.setRemoteProxy(l.getRemoteProxy());
//		languageServer.
//		
//	}
//
//}
