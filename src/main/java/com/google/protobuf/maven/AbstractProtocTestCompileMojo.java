package com.google.protobuf.maven;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * An abstract base mojo configuration for using {@code protoc} compiler with the test sources.
 *
 * @since 0.3.3
 */
public abstract class AbstractProtocTestCompileMojo extends AbstractProtocMojo {

    /**
     * The source directories containing the test {@code .proto} definitions to be compiled.
     */
    @Parameter(
            required = true,
            defaultValue = "${basedir}/src/test/proto"
    )
    private File protoTestSourceRoot;

    /**
     * This is the directory into which the (optional) descriptor set file will be created.
     *
     * @since 0.3.0
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-test-resources/protobuf/descriptor-sets"
    )
    private File descriptorSetOutputDirectory;

    /**
     * If generated descriptor set is to be attached to the build, specifies an optional classifier.
     *
     * @since 0.4.1
     */
    @Parameter(
            required = false,
            defaultValue = "test"
    )
    protected String descriptorSetClassifier;

    @Override
    protected void doAttachProtoSources() {
        projectHelper.addTestResource(project, getProtoSourceRoot().getAbsolutePath(),
                ImmutableList.copyOf(getIncludes()), ImmutableList.copyOf(getExcludes()));
    }

    @Override
    protected void doAttachGeneratedFiles() {
        final File outputDirectory = getOutputDirectory();
        project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
        if (writeDescriptorSet) {
            final File descriptorSetFile = new File(getDescriptorSetOutputDirectory(), descriptorSetFileName);
            projectHelper.attachArtifact(project, "test-protobin", descriptorSetClassifier, descriptorSetFile);
        }
        buildContext.refresh(outputDirectory);
    }

    @Override
    protected List<Artifact> getDependencyArtifacts() {
        return project.getTestArtifacts();
    }

    @Override
    protected File getDescriptorSetOutputDirectory() {
        return descriptorSetOutputDirectory;
    }

    @Override
    protected File getProtoSourceRoot() {
        return protoTestSourceRoot;
    }
}
