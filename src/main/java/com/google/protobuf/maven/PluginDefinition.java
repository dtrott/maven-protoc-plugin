package com.google.protobuf.maven;

import java.io.File;

/**
 * Represents a protoc plugin. Provides the necessary information
 * to specify the output directory and optionally specify the executable.
 *
 * If a plugin is generating code not to be build by Maven, it can be
 * excluded form the project by setting s projectSource to false.
 *
 */
public class PluginDefinition {
    private String name;
    private File outputDirectory;
    private String pluginCommand;
    private boolean projectSource = true;

    public String getName() {
        return name;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public String getPluginCommand() {
        return pluginCommand;
    }

    public boolean isProjectSource() {
        return projectSource;
    }
}
