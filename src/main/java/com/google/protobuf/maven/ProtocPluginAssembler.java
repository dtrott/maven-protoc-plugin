package com.google.protobuf.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates an executable {@code protoc} plugin (written in Java) from a {@link ProtocPlugin} specification.
 *
 * @since 0.3.0
 */
public class ProtocPluginAssembler {

    private final RepositorySystem repoSystem;

    private final RepositorySystemSession repoSystemSession;

    private final List<RemoteRepository> remoteRepos = new ArrayList<RemoteRepository>();

    private final ProtocPlugin pluginDefinition;

    private final File pluginDirectory;

    private final List<File> resolvedJars = new ArrayList<File>();

    private final File pluginExecutableFile;

    private final Log log;

    public ProtocPluginAssembler(
            final ProtocPlugin pluginDefinition,
            final RepositorySystem repoSystem,
            final RepositorySystemSession repoSystemSession,
            final List<RemoteRepository> remoteRepos,
            final File pluginDirectory,
            final Log log) {
        this.repoSystem = repoSystem;
        this.repoSystemSession = repoSystemSession;
        this.remoteRepos.addAll(remoteRepos);
        this.pluginDefinition = pluginDefinition;
        this.pluginDirectory = pluginDirectory;
        this.pluginExecutableFile = pluginDefinition.getPluginExecutableFile(pluginDirectory);
        this.log = log;
    }

    /**
     * Resolves the plugin's dependencies to the local Maven repository and builds the plugin executable.
     *
     * @throws MojoExecutionException if plugin executable could not be built.
     */
    public void execute() throws MojoExecutionException {
        pluginDefinition.validate(log);

        if (log.isDebugEnabled()) {
            log.debug("plugin definition: " + pluginDefinition);
        }

        resolvePluginDependencies();

        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            buildWindowsPlugin();
            copyWinRun4JExecutable();
        } else {
            buildUnixPlugin();
            pluginExecutableFile.setExecutable(true);
        }
    }

    private void buildWindowsPlugin() throws MojoExecutionException {
        createPluginDirectory();

        // Try to locate jvm.dll based on pluginDefinition's javaHome property
        final File javaHome = new File(pluginDefinition.getJavaHome());
        final File jvmLocation = findJvmLocation(javaHome,
                "jre/bin/server/jvm.dll",
                "bin/server/jvm.dll",
                "jre/bin/client/jvm.dll",
                "bin/client/jvm.dll");
        final File winRun4JIniFile = new File(pluginDirectory, pluginDefinition.getPluginName() + ".ini");

        if (log.isDebugEnabled()) {
            log.debug("javaHome=" + javaHome.getAbsolutePath());
            log.debug("jvmLocation=" + (jvmLocation != null ? jvmLocation.getAbsolutePath() : "(none)"));
            log.debug("winRun4JIniFile=" + winRun4JIniFile.getAbsolutePath());
            log.debug("winJvmDataModel=" + pluginDefinition.getWinJvmDataModel());
        }

        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(winRun4JIniFile));
            if (jvmLocation != null) {
                out.println("vm.location=" + jvmLocation.getAbsolutePath());
            }
            int index = 1;
            for (final File resolvedJar : resolvedJars) {
                out.println("classpath." + index + "=" + resolvedJar.getAbsolutePath());
                index++;
            }
            out.println("main.class=" + pluginDefinition.getMainClass());

            index = 1;
            for (final String arg : pluginDefinition.getArgs()) {
                out.println("arg." + index + "=" + arg);
                index++;
            }

            index = 1;
            for (final String jvmArg : pluginDefinition.getJvmArgs()) {
                out.println("vmarg." + index + "=" + jvmArg);
                index++;
            }

            out.println("vm.version.min=1.6");

            // keep from logging to stdout (the default)
            out.println("log.level=none");
            out.println("[ErrorMessages]");
            out.println("show.popup=false");
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Could not write WinRun4J ini file: " + winRun4JIniFile.getAbsolutePath(), e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File findJvmLocation(File javaHome, String... paths) {
        for (String path : paths) {
            File jvmLocation = new File(javaHome, path);
            if (jvmLocation.isFile()) {
                return jvmLocation;
            }
        }
        return null;
    }

    private void copyWinRun4JExecutable() throws MojoExecutionException {
        final String executablePath = getWinrun4jExecutablePath();
        final URL url = Thread.currentThread().getContextClassLoader().getResource(executablePath);
        if (url == null) {
            throw new MojoExecutionException(
                    "Could not locate WinRun4J executable at path: " + executablePath);
        }
        try {
            FileUtils.copyURLToFile(url, pluginExecutableFile);
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Could not copy WinRun4J executable to: " + pluginExecutableFile.getAbsolutePath(), e);
        }
    }

    private void buildUnixPlugin() throws MojoExecutionException {
        createPluginDirectory();

        final File javaLocation = new File(pluginDefinition.getJavaHome(), "bin/java");

        if (log.isDebugEnabled()) {
            log.debug("javaLocation=" + javaLocation.getAbsolutePath());
        }

        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(pluginExecutableFile));
            out.println("#!/bin/sh");
            out.println();
            out.print("CP=");
            for (int i = 0; i < resolvedJars.size(); i++) {
                if (i > 0) {
                    out.print(":");
                }
                out.print("\"" + resolvedJars.get(i).getAbsolutePath() + "\"");
            }
            out.println();
            out.print("ARGS=\"");
            for (final String arg : pluginDefinition.getArgs()) {
                out.print(arg + " ");
            }
            out.println("\"");
            out.print("JVMARGS=\"");
            for (final String jvmArg : pluginDefinition.getJvmArgs()) {
                out.print(jvmArg + " ");
            }
            out.println("\"");
            out.println();
            out.println("\"" + javaLocation.getAbsolutePath() + "\" $JVMARGS -cp $CP "
                    + pluginDefinition.getMainClass() + " $ARGS");
            out.println();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write plugin script file: " + pluginExecutableFile, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void createPluginDirectory() throws MojoExecutionException {
        pluginDirectory.mkdirs();
        if (!pluginDirectory.isDirectory()) {
            throw new MojoExecutionException("Could not create protoc plugin directory: "
                    + pluginDirectory.getAbsolutePath());
        }
    }

    private void resolvePluginDependencies() throws MojoExecutionException {
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(pluginDefinition.asDependency());
        for (final RemoteRepository remoteRepo : remoteRepos) {
            collectRequest.addRepository(remoteRepo);
        }

        try {
            final DependencyNode node = repoSystem.collectDependencies(repoSystemSession, collectRequest).getRoot();
            final DependencyRequest request = new DependencyRequest(node, null);
            repoSystem.resolveDependencies(repoSystemSession, request);
            final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);

            resolvedJars.addAll(nlg.getFiles());

            if (log.isDebugEnabled()) {
                log.debug("resolved jars: " + resolvedJars);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    private String getWinrun4jExecutablePath() {
        return "winrun4j/WinRun4J" + pluginDefinition.getWinJvmDataModel() + ".exe";
    }
}
