package com.google.protobuf.maven.toolchain;

import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Based on {@code org.apache.maven.toolchain.java.DefaultJavaToolChain}.
 *
 * @author Sergei Ivanov
 * @since 0.2.0
 */
public class DefaultProtobufToolchain extends DefaultToolchain implements ProtobufToolchain {

    public static final String KEY_PROTOC_EXECUTABLE = "protocExecutable";
    public static final String KEY_PLUGIN_DIRECTORY = "pluginDirectory";

    protected DefaultProtobufToolchain(ToolchainModel model, Logger logger) {
        super(model, "protobuf", logger);
    }

    private String protocExecutable;
    private File pluginDirectory;

    public String findTool(String toolName) {
        if ("protoc".equals(toolName)) {
            File protoc = new File(FileUtils.normalize(getProtocExecutable()));
            if (protoc.exists()) {
                return protoc.getAbsolutePath();
            }
        }
        return null;
    }

    public String getProtocExecutable() {
        return this.protocExecutable;
    }

    public void setProtocExecutable(String protocExecutable) {
        this.protocExecutable = protocExecutable;
    }

    public File getPluginDirectory() {
        return this.pluginDirectory;
    }

    public void setPluginDirectory(File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    public String toString() {
        return "PROTOC[" + getProtocExecutable() + "]";
    }
}
