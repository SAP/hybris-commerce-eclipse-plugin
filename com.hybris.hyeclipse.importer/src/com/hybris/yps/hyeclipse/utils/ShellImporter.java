package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.hybris.yps.hyeclipse.Activator;
import com.hybris.yps.hyeclipse.preferences.AntJavaRuntime;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceConstants;
import com.hybris.yps.hyeclipse.preferences.HyEclipsePreferenceInitializer;
import com.hybris.yps.hyeclipse.wizards.ImportJobData;
import com.hybris.yps.hyeclipse.wizards.Messages;

public class ShellImporter {

	private static final String ANT_LOG = "ant.ser.log"; //$NON-NLS-1$

	private MessageConsoleStream out;
	private MessageConsoleStream err;
	private final BlockingDeque<String> deque = new LinkedBlockingDeque<>();
	private final IVMInstall javaRuntime;

	/** Uses the saved import JDK selection. */
	public ShellImporter() {
		this(null);
	}

	/** @param javaRuntime JDK captured for this import, or null to resolve the saved selection */
	public ShellImporter(final IVMInstall javaRuntime) {
		this.javaRuntime = javaRuntime;
	}

	/**
	 * Updates dependencies, then imports and configures the selected extensions.
	 * @param data selected platform and import options
	 * @param monitor progress and cancellation monitor
	 * @throws IOException if Ant fails
	 * @throws CoreException if workspace import fails
	 * @throws InterruptedException if interrupted
	 */
	public void resetProjectsFromLocalExtensions(final ImportJobData data, final IProgressMonitor monitor)
			throws IOException, CoreException, InterruptedException {
		try (final ProjectJavaRuntime.ChangeScope change = ProjectJavaRuntime.beginChange(monitor)) {
			final IVMInstall vm = javaRuntime != null ? javaRuntime : AntJavaRuntime.getVMInstall();
			new ShellImporter(vm).executeAntTask(data.platformDir(), monitor);
			new Importer(null, vm).resetProjectsFromLocalExtensions(data.platformDir(), monitor, data.fixClasspath(),
					data.removeHybrisBuilder(), data.createWorkingSet(), data.useMultiThread(), data.skipJarScanning());
		}
	}

	public boolean isWindows() {
		return Platform.OS_WIN32.equals(Platform.getOS());
	}

	String defaultShell() {
		if (isWindows()) {
			return HyEclipsePreferenceInitializer.getWindowsDefaultShell();
		}
		final String shell = Activator.getDefault().getPreferenceStore()
				.getString(HyEclipsePreferenceConstants.P_DEFAULT_SHELL);
		return StringUtils.isNotBlank(shell) ? shell.trim() : HyEclipsePreferenceInitializer.getDefaultShell();
	}

	String antGoal() {
		final String goal = Activator.getDefault().getPreferenceStore()
				.getString(HyEclipsePreferenceConstants.P_ANT_GOAL);
		return StringUtils.isNotBlank(goal) ? goal.trim() : HyEclipsePreferenceConstants.DEFAULT_ANT_GOAL;
	}

	List<String> antCommand(final String shell, final boolean windows) {
		final String goal = antGoal();
		// Reapply the selected JDK after startup files and setantenv can change JAVA_HOME/PATH.
		if (windows) {
			// Delayed expansion preserves PATH additions made by setantenv.bat on this command line.
			final String javaEnvironment = "set \"JAVA_HOME=!" + AntJavaRuntime.ANT_JAVA_HOME
					+ "!\" && set \"PATH=!" + AntJavaRuntime.ANT_JAVA_HOME + "!\\bin;!PATH!\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return Arrays.asList(HyEclipsePreferenceInitializer.getWindowsDefaultShell(),
					"/v:on", "/c", javaEnvironment + " && call setantenv.bat && " + javaEnvironment
							+ " && java -version && ant " + goal); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		final String javaEnvironment = "export JAVA_HOME=\"$" + AntJavaRuntime.ANT_JAVA_HOME
				+ "\" && export PATH=\"$JAVA_HOME/bin:$PATH\""; //$NON-NLS-1$ //$NON-NLS-2$
		return Arrays.asList(shell, "--login", "-c", javaEnvironment + " && . ./setantenv.sh && "
				+ javaEnvironment + " && java -version && exec ant " + goal); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	ProcessBuilder antProcess(final File platformHome) throws IOException {
		final File javaHome = javaRuntime != null ? AntJavaRuntime.getJavaHome(javaRuntime)
				: AntJavaRuntime.getJavaHome();
		final ProcessBuilder builder = new ProcessBuilder(antCommand(defaultShell(), isWindows()));
		builder.directory(platformHome);
		final Map<String, String> environment = builder.environment();
		environment.put(AntJavaRuntime.JAVA_HOME, javaHome.getAbsolutePath());
		environment.put(AntJavaRuntime.ANT_JAVA_HOME, javaHome.getAbsolutePath());
		// Windows environment keys are case-insensitive; avoid creating both Path and PATH.
		final String pathKey = environment.keySet().stream().filter(key -> key.equalsIgnoreCase(AntJavaRuntime.PATH))
				.findFirst().orElse(AntJavaRuntime.PATH);
		environment.put(pathKey, new File(javaHome, AntJavaRuntime.BIN_DIRECTORY).getAbsolutePath()
				+ File.pathSeparator + StringUtils.defaultString(environment.get(pathKey)));
		return builder;
	}

	/**
	 * Runs the dependency update target and records its complete output.
	 * @param platformHome Commerce platform directory
	 * @param monitor cancellation monitor
	 * @return console output for future processing
	 * @throws IOException if process execution or logging fails
	 * @throws InterruptedException if interrupted
	 */
	public Collection<String> executeAntTask(final File platformHome, final IProgressMonitor monitor)
			throws IOException, InterruptedException {
		deque.clear();
		final Display display = Display.getDefault();
		display.syncExec(() -> {
			final MessageConsole console = findConsole(Messages.ShellImporter_console_title);
			out = console.newMessageStream();
			err = console.newMessageStream();
			err.setColor(display.getSystemColor(SWT.COLOR_RED));
		});
		try (final MessageConsoleStream output = out; final MessageConsoleStream errors = err) {
			final ProcessBuilder builder = antProcess(platformHome);
			monitor.subTask(Messages.ShellImporter_updating_dependencies);
			try {
				AntProcessRunner.execute(builder, monitor, this::consumeMsg, this::consumeErr);
			} finally {
				// Readers have stopped, including on failure or cancellation.
				FileUtils.writeLines(new File(platformHome, ANT_LOG), deque);
			}
		}
		return deque;
	}

	private void consumeMsg(String msg) {
		out.println(msg);
		deque.add(msg);
	}

	private void consumeErr(String msg) {
		err.println(msg);
		deque.add(msg);
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		MessageConsole myConsole = null;
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName()) && existing[i] instanceof MessageConsole) {
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
