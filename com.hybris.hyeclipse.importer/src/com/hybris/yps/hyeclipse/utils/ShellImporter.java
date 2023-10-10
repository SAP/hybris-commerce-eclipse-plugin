package com.hybris.yps.hyeclipse.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class ShellImporter {

	private String ANT_INIT_CONFIG = "updateMavenDependencies";
//	private String ANT_CLEAN_ALL = "clean all";
	private String ANT_LOG = "ant.ser.log";

	private MessageConsoleStream out;
	private MessageConsoleStream err;
	private BlockingDeque<String> deque = new LinkedBlockingDeque<>();

	private static class StreamGobbler implements Runnable {
		private InputStream inputStream;
		private Consumer<String> consumer;

		public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
		}
	}

	public void resetProjectsFromLocalExtensions(final File platformHome, final IProgressMonitor monitor,
			final boolean fixClasspath, final boolean removeHybrisGenerator, final boolean createWorkingSets,
			final boolean useMultiThread, final boolean skipJarScanning) {

		executeAntTask(platformHome);
	}

	public boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	private final String defaultShell() {
		String shell = null;
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			if (System.getenv("ComSpec") != null && !"".equals(System.getenv("ComSpec").trim())) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				shell = System.getenv("ComSpec").trim(); //$NON-NLS-1$
			} else {
				shell = "cmd.exe"; //$NON-NLS-1$
			}
		}
		if (shell == null) {

			shell = "/bin/bash"; //$NON-NLS-1$
		}

		return shell;
	}

	public void executeAntTask(File platformHome) {
		deque.clear();
		MessageConsole myConsole = findConsole("SAP Commerce Build");
		out = myConsole.newMessageStream();
		err = myConsole.newMessageStream();
		err.setColor(new Color(new RGB(255, 0, 0)));
		ProcessBuilder processBuilder = new ProcessBuilder();
		String sh = defaultShell();
		if (isWindows()) {
			processBuilder.command(sh, "/c", "ant");
		} else {
			processBuilder.command(sh, "--login", "-c", ". ./setantenv.sh ; ant " + ANT_INIT_CONFIG);
		}
		processBuilder.directory(platformHome);
		processBuilder.environment().putAll(System.getenv());
		Process process;
		try {
			process = processBuilder.start();
			out.println(sh);
			out.println("------------- Start Run Test Case -------------");
			StreamGobbler sout = new StreamGobbler(process.getInputStream(), m -> consumeMsg(m));
			StreamGobbler serr = new StreamGobbler(process.getErrorStream(), m -> consumeErr(m));
			ExecutorService pool = Executors.newFixedThreadPool(3);
			CompletionService<Void> completionServe = new ExecutorCompletionService<>(pool);
			completionServe.submit(sout, null);
			completionServe.submit(serr, null);
			int exitCode = process.waitFor();
			out.println(String.format("-------------  End Run Test Case: (%s)  -------------", exitCode));
			flushToFile(platformHome.toPath().resolve(ANT_LOG));
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void consumeMsg(String msg) {
		Optional.ofNullable(out).ifPresent(out -> {
			out.println(msg);
			deque.add(msg);
		});
	}

	private void consumeErr(String msg) {
		Optional.ofNullable(err).ifPresent(out -> {
			out.println(msg);
			deque.add(msg);
		});
	}

	private void flushToFile(Path path) throws IOException {
		File log = path.toFile();
		FileUtils.forceMkdirParent(path.toFile());
		FileUtils.writeLines(log, deque);
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		MessageConsole myConsole = null;
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				myConsole = (MessageConsole) existing[i];
				break;
			}
		}
		if (myConsole == null) {
			myConsole = new MessageConsole(name, null);
			conMan.addConsoles(new IConsole[] { myConsole });
		}
		conMan.showConsoleView(myConsole);
		return myConsole;
	}

}
