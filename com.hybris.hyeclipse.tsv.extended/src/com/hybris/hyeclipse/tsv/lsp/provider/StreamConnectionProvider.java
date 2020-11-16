package com.hybris.hyeclipse.tsv.lsp.provider;

import java.io.IOException;

import com.hybris.hyeclipse.tsv.lsp.TSVLanguageServer;

public class StreamConnectionProvider extends AbstractConnectionProvider {
	private final static TSVLanguageServer _INST = new TSVLanguageServer();

	public StreamConnectionProvider() {
		super(_INST);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void start() throws IOException {
		super.start();
		_INST.setClient(super.launcher.getRemoteProxy());

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
