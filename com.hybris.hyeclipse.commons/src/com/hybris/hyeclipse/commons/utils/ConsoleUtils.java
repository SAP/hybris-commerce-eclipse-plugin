package com.hybris.hyeclipse.commons.utils;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Utility class for plugin console handling.
 */
public final class ConsoleUtils {
	
	/**
	 * Plugin console name
	 */
	private static final String PLUGIN = "hyeclipse";

	/**
	 * Private in order to avoid class initialization.
	 */
	private ConsoleUtils() { /* intentionally empty. */ }
	
	/**
	 * Returns message console with specified name, if not exists, creates new
	 * one
	 *
	 * @param name
	 *            name of the console
	 * @return plugin console
	 */
	public static MessageConsole findConsole(final String name) {
		final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		final IConsole[] existing = consoleManager.getConsoles();
		
		Optional<IConsole> existingConsole = Arrays.stream(existing).filter(console -> name.equals(console.getName())).findFirst();
		if( existingConsole.isPresent() ) {
			return (MessageConsole) existingConsole.get();
		}
	
		final MessageConsole console = new MessageConsole(name, null);
		consoleManager.addConsoles(new IConsole[] { console });
		consoleManager.showConsoleView(console);
		
		return console;
	}

	/**
	 * Returns {@link MessageConsoleStream} to the plugin console
	 *
	 * @return message console stream
	 */
	public static MessageConsoleStream getConsoleStream() {
		final MessageConsole pluginConsole = findConsole(PLUGIN);
		return pluginConsole.newMessageStream();
	}
	
	/**
	 * Prints new line to the console
	 */
	public static void printLine() {
		getConsoleStream().println();
	}
	
	/**
	 * Prints message to the console.
	 * 
	 * @param message message to print.
	 */
	public static void printMessage(final String message) {
		getConsoleStream().println(message);
	}

	/**
	 * Prints error message to the console.
	 * 
	 * @param message error message to print
	 */
	public static void printError(final String message) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		final MessageConsoleStream consoleStream = getConsoleStream();
		final Color color = new Color(display, new RGB(255, 0, 0));
		
		consoleStream.setColor(color);
		consoleStream.println(message);
	}
}