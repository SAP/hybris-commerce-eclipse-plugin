package com.hybris.hyeclipse.tsv.lsp.provider;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class AbstractConnectionProvider  implements StreamConnectionProvider {

	private InputStream inputStream  ;
	private InputStream errorStream  ;
	private OutputStream outputStream;
	private LanguageServer ls;
	protected Launcher<LanguageClient> launcher;
	
	public AbstractConnectionProvider(LanguageServer ls) {
		this.ls = ls;
	}
	
	@Override
	public void start() throws IOException {
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in2 = new PipedInputStream();
		PipedOutputStream out2 = new PipedOutputStream();
		
		in.connect(out2);
		out.connect(in2);
	
		launcher = LSPLauncher.createServerLauncher(ls, in2, out2);
		inputStream = in;
		outputStream = out;
		errorStream = new PipedInputStream();
		launcher.startListening();
	}

	@Override
	public InputStream getInputStream() {
		return new FilterInputStream(inputStream) {
			@Override
			public int read() throws IOException {
				int res = super.read();
				System.err.print((char) res);
				return res;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int bytes = super.read(b, off, len);
				byte[] payload = new byte[bytes];
				System.arraycopy(b, off, payload, 0, bytes);
				System.err.print(new String(payload));
				return bytes;
			}

			@Override
			public int read(byte[] b) throws IOException {
				int bytes = super.read(b);
				byte[] payload = new byte[bytes];
				System.arraycopy(b, 0, payload, 0, bytes);
				System.err.print(new String(payload));
				return bytes;
			}
		};
	}

	@Override
	public OutputStream getOutputStream() {
		return new FilterOutputStream(outputStream) {
			@Override
			public void write(int b) throws IOException {
				System.err.print((char) b);
				super.write(b);
			}

			@Override
			public void write(byte[] b) throws IOException {
				System.err.print(new String(b));
				super.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				byte[] actual = new byte[len];
				System.arraycopy(b, off, actual, 0, len);
				System.err.print(new String(actual));
				super.write(b, off, len);
			}
		};
	}

	@Override
	public void stop() {
	}

	@Override
	public InputStream getErrorStream() {
		return errorStream;
	}
	
}