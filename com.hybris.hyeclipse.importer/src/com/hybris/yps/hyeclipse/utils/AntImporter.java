package com.hybris.yps.hyeclipse.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.listener.BigProjectLogger;
import org.apache.tools.ant.taskdefs.optional.sound.AntSoundPlayer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.hybris.hyeclipse.commons.Activator;

public class AntImporter {
	
    private StringBuffer logBuffer = new StringBuffer();
	
    private class AntTestListener implements BuildListener {
        private int logLevel;
        private StringBuffer fullLogBuffer = new StringBuffer();

        /**
         * Constructs a test listener which will ignore log events
         * above the given level.
         */
        public AntTestListener(int logLevel, StringBuffer antTaskLog) {
            this.logLevel = logLevel;
            this.fullLogBuffer = antTaskLog;
        }

        /**
         * Fired before any targets are started.
         */
        public void buildStarted(BuildEvent event) {
        }

        /**
         * Fired after the last target has finished. This event
         * will still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void buildFinished(BuildEvent event) {
        }

        /**
         * Fired when a target is started.
         *
         * @see BuildEvent#getTarget()
         */
        public void targetStarted(BuildEvent event) {
            //System.out.println("targetStarted " + event.getTarget().getName());
        }

        /**
         * Fired when a target has finished. This event will
         * still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void targetFinished(BuildEvent event) {
            //System.out.println("targetFinished " + event.getTarget().getName());
        }

        /**
         * Fired when a task is started.
         *
         * @see BuildEvent#getTask()
         */
        public void taskStarted(BuildEvent event) {
            //System.out.println("taskStarted " + event.getTask().getTaskName());
        }

        /**
         * Fired when a task has finished. This event will still
         * be throw if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void taskFinished(BuildEvent event) {
            //System.out.println("taskFinished " + event.getTask().getTaskName());
        }

        /**
         * Fired whenever a message is logged.
         *
         * @see BuildEvent#getMessage()
         * @see BuildEvent#getPriority()
         */
        public void messageLogged(BuildEvent event) {
        	fullLogBuffer.append(event.getMessage());
            if (event.getPriority() > logLevel) {
                // ignore event
                return;
            }

            if (event.getPriority() == Project.MSG_INFO
                || event.getPriority() == Project.MSG_WARN
                || event.getPriority() == Project.MSG_ERR) {
                logBuffer.append(event.getMessage());
                // 
            }
        }
    }
	
	public void resetProjectsFromLocalExtensions(final File platformHome, final IProgressMonitor monitor,
			final boolean fixClasspath, final boolean removeHybrisGenerator, final boolean createWorkingSets,
			final boolean useMultiThread, final boolean skipJarScanning) {
		
		executeAntTask(platformHome.getAbsolutePath());
		
	}
	
	public boolean executeAntTask(String buildXmlFileFullPath) {
		
		Activator.log(System.getProperty("ANT_HOME"));
        logBuffer = new StringBuffer();
        boolean success = false;
        MessageConsole myConsole = findConsole("SAP Commerce Build");
        MessageConsoleStream out = myConsole.newMessageStream();
        
        AntSoundPlayer antSoundPlayer = new AntSoundPlayer();
        BigProjectLogger bigLogger = new BigProjectLogger();
        bigLogger.setOutputPrintStream(System.out);
        bigLogger.setErrorPrintStream(System.err);
        AntTestListener myListener = new AntTestListener(Project.MSG_INFO, logBuffer);
        
        Project project = new Project();
        project.setBasedir(buildXmlFileFullPath);
        project.setNewProperty("PLATFORM_HOME", buildXmlFileFullPath);
        project.setJavaVersionProperty();
        Path buildPath = Paths.get(buildXmlFileFullPath, "build.xml");
        File buildFile = buildPath.toFile();



        project.setUserProperty("ant.file", buildFile.getAbsolutePath());
        project.addBuildListener(antSoundPlayer);
        project.addBuildListener(bigLogger);
        project.addBuildListener(myListener);


        try {
            project.fireBuildStarted();
            project.init();
            ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
            project.addReference("ant.projectHelper", projectHelper);

            projectHelper.parse(project, buildFile);
//            project.setNewProperty("test", className.trim());
//            if (!(methodName.equals(""))) {
//                project.setNewProperty("method", methodName.trim());
//            }
//            if ("debug".equals(mode)) {
                project.setNewProperty("debug", "true");
//            }   

            project.executeTarget("test");
            project.fireBuildFinished(null);
            project.getBuildListeners();
            out.println("------------- Start Run Test Case -------------");
            out.println( logBuffer.toString());
            out.println("-------------  End Run Test Case  -------------");

            success = true;
        } catch (BuildException buildException) {
            project.fireBuildFinished(buildException);
        }

        return success;
    }
	
	private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++)
            if (name.equals(existing[i].getName()))
                return (MessageConsole) existing[i];
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }
	
	

}
