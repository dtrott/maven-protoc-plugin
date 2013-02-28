package com.google.protobuf.maven;

import org.apache.maven.plugin.logging.Log;
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
 *
 * @since 0.3.0
 */
public class ProtocPlugin {

    private static final String DATA_MODEL_SYSPROP = "sun.arch.data.model";
    private static final String WIN_JVM_DATA_MODEL_32 = "32";
    private static final String WIN_JVM_DATA_MODEL_64 = "64";


    private String id;

    private String groupId;

    private String artifactId;

    private String version;

    private String scope = "runtime";

    private String mainClass;

    private String javaHome;

    // Assuming we're running a HotSpot JVM, use the data model of the
    // current JVM as the default. This property is only relevant on
    // Windows where we need to pick the right version of the WinRun4J executable.
    private String winJvmDataModel;

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

    public void setJavaHome(final String javaHome) {
        this.javaHome = javaHome;
    }

    public String getWinJvmDataModel() {
        return winJvmDataModel;
    }

    public String getPluginName() {
        return "protoc-gen-" + id;
    }

    /**
     * Validate the state of this plugin specification.
     * @throws IllegalStateException if properties are incorrect or are missing
     */
    public void validate(final Log log) {
        checkState(id != null, "id must be set in protocPlugin definition");
        checkState(groupId != null, "groupId must be set in protocPlugin definition");
        checkState(artifactId != null, "artifactId must be set in protocPlugin definition");
        checkState(version != null, "version must be set in protocPlugin definition");
        checkState(mainClass != null, "mainClass must be set in protocPlugin definition");
        checkState(javaHome != null && new File(javaHome).isDirectory(), "javaHome is invalid: " + javaHome);
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {

            // If winJvmDataModel isn't set explicitly, try to guess the architecture
            // by looking at the directories in the JDK/JRE javaHome points at.
            // If that fails, try to figure out from the currently running JVM.

            if (winJvmDataModel != null) {
                checkState(winJvmDataModel.equals(WIN_JVM_DATA_MODEL_32) || winJvmDataModel.equals(WIN_JVM_DATA_MODEL_64),
                        "winJvmDataModel must be '32' or '64'");
            } else if (archDirectoryExists("amd64")) {
                winJvmDataModel = WIN_JVM_DATA_MODEL_64;
                if (log.isDebugEnabled()) {
                    log.debug("detected 64-bit JVM from directory structure");
                }
            } else if (archDirectoryExists("i386")) {
                winJvmDataModel = WIN_JVM_DATA_MODEL_32;
                if (log.isDebugEnabled()) {
                    log.debug("detected 32-bit JVM from directory structure");
                }
            } else if (System.getProperty(DATA_MODEL_SYSPROP) != null){
                winJvmDataModel = System.getProperty(DATA_MODEL_SYSPROP);
                if (log.isDebugEnabled()) {
                    log.debug("detected " + winJvmDataModel + "-bit JVM from system property " + DATA_MODEL_SYSPROP);
                }
            } else {
                winJvmDataModel = WIN_JVM_DATA_MODEL_32;
                if (log.isDebugEnabled()) {
                    log.debug("defaulting to 32-bit JVM");
                }
            }
        }
    }

    private boolean archDirectoryExists(String arch) {
        return javaHome != null
                && (new File(javaHome, "jre/lib/" + arch).isDirectory()
                    || new File(javaHome, "lib/" + arch).isDirectory());
    }

    /**
     * Returns the generated plugin executable path.
     *
     * @param pluginDirectory directory where plugins will be created
     * @return file handle for the plugin executable.
     */
    public File getPluginExecutableFile(final File pluginDirectory) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return new File(pluginDirectory, getPluginName() + ".exe");
        } else {
            return new File(pluginDirectory, getPluginName());
        }
    }

    public Dependency asDependency() {
        return new Dependency(new DefaultArtifact(groupId, artifactId, "jar", version), scope);
    }

    @Override
    public String toString() {
        return "ProtocPlugin{" +
                "id='" + id + '\'' +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", javaHome='" + javaHome + '\'' +
                ", winJvmDataModel='" + winJvmDataModel + '\'' +
                ", args=" + args +
                ", jvmArgs=" + jvmArgs +
                '}';
    }
}
