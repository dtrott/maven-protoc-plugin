package com.google.protobuf.maven.toolchain;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;

/**
 * Based on {@code org.apache.maven.toolchain.java.DefaultJavaToolchainFactory}.
 *
 * @author Sergei Ivanov
 * @since 0.2.0
 */
public class DefaultProtobufToolchainFactory implements ToolchainFactory, LogEnabled {

    private Logger logger;

    public ToolchainPrivate createToolchain(ToolchainModel model) throws MisconfiguredToolchainException {
        if (model == null) {
            return null;
        }
        DefaultProtobufToolchain toolchain = new DefaultProtobufToolchain(model, logger);
        Xpp3Dom dom = (Xpp3Dom) model.getConfiguration();
        Xpp3Dom protocExecutable = dom.getChild(DefaultProtobufToolchain.KEY_PROTOC_EXECUTABLE);
        if (protocExecutable == null) {
            throw new MisconfiguredToolchainException(
                    "Protobuf toolchain without the "
                            + DefaultProtobufToolchain.KEY_PROTOC_EXECUTABLE
                            + " configuration element.");
        }
        File normal = new File(FileUtils.normalize(protocExecutable.getValue()));
        if (normal.exists()) {
            toolchain.setProtocExecutable(FileUtils.normalize(protocExecutable.getValue()));
        } else {
            throw new MisconfiguredToolchainException(
                    "Non-existing protoc executable at " + normal.getAbsolutePath());
        }

        //now populate the provides section.
        dom = (Xpp3Dom) model.getProvides();
        Xpp3Dom[] provides = dom.getChildren();
        for (final Xpp3Dom provide : provides) {
            String key = provide.getName();
            String value = provide.getValue();
            if (value == null) {
                throw new MisconfiguredToolchainException(
                        "Provides token '" + key + "' doesn't have any value configured.");
            }
            if ("version".equals(key)) {
                toolchain.addProvideToken(key,
                        RequirementMatcherFactory.createVersionMatcher(value));
            } else {
                toolchain.addProvideToken(key,
                        RequirementMatcherFactory.createExactMatcher(value));
            }
        }
        return toolchain;
    }

    public ToolchainPrivate createDefaultToolchain() {
        return null;
    }

    protected Logger getLogger() {
        return logger;
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }
}
