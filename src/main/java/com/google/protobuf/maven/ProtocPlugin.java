package com.google.protobuf.maven;

import org.codehaus.plexus.util.Os;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Describes a {@code protoc} plugin that is written in Java and
 * assembled from resolved artifacts at runtime.
 * The state is populated from the Maven plugin's configuration.
 */
public class ProtocPlugin {

    private String id;

    private String groupId;

    private String artifactId;

    private String version;

    private String scope = "runtime";

    private String mainClass;

    private String javaHome;

    private List<String> args;

    private List<String> jvmArgs;

    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getScope() {
        return scope;
    }

    public String getMainClass() {
        return mainClass;
    }

    public List<String> getArgs() {
        return (args != null) ? args : Collections.<String>emptyList();
    }

    public List<String> getJvmArgs() {
        return (jvmArgs != null) ? jvmArgs : Collections.<String>emptyList();
    }

    public String getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getPluginName() {
        return "protoc-gen-" + id;
    }

    /**
     * Validate the state of this plugin specification.
     * @throws IllegalStateException if properties are incorrect or are missing
     */
    public void validate() {
        checkState(id != null, "id must be set in protocPlugin definition");
        checkState(groupId != null, "groupId must be set in protocPlugin definition");
        checkState(artifactId != null, "artifactId must be set in protocPlugin definition");
        checkState(version != null, "version must be set in protocPlugin definition");
        checkState(mainClass != null, "mainClass must be set in protocPlugin definition");
        checkState(javaHome != null && new File(javaHome).isDirectory(), "javaHome must is invalid: " + javaHome);
    }

    /**
     * Returns the generated plugin executable path.
     * @param pluginDirectory directory where plugins will be created
     * @return
     */
    public File getPluginExecutableFile(File pluginDirectory) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return new File(pluginDirectory, getPluginName() + ".exe");
        } else {
            return new File(pluginDirectory, getPluginName());
        }
    }

    public Dependency asDependency() {
        return new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), scope);
    }

}
