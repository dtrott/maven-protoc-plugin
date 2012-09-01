package com.google.protobuf.maven;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.List;

/**
 * This mojo executes the {@code protoc} compiler for generating main java sources
 * from protocol buffer definitions. It also searches dependency artifacts for
 * {@code .proto} files and includes them in the {@code proto_path} so that they can be
 * referenced. Finally, it adds the {@code .proto} files to the project as resources so
 * that they are included in the final artifact.
 *
 * @phase generate-sources
 * @goal compile
 * @requiresDependencyResolution compile
 * @threadSafe
 */

public final class ProtocCompileMojo extends AbstractProtocMojo {

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter default-value="${basedir}/src/main/proto"
     * @required
     */
    private File protoSourceRoot;

    /**
     * This is the directory into which the {@code .java} will be created.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/protoc"
     * @required
     */
    private File outputDirectory;


    /**
     * This is the directory into which the (optional) descriptor set file will be created.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/protoc-descriptor-sets"
     * @required
     */
    private File descriptorSetOutputDirectory;

    @Override
    protected List<Artifact> getDependencyArtifacts() {
        // TODO(gak): maven-project needs generics
        @SuppressWarnings("unchecked")
        List<Artifact> compileArtifacts = project.getCompileArtifacts();
        return compileArtifacts;
    }

    @Override
    protected File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    protected File getDescriptorSetOutputDirectory() {
        return descriptorSetOutputDirectory;
    }

    @Override
    protected File getProtoSourceRoot() {
        return protoSourceRoot;
    }

    @Override
    protected void attachFiles() {
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        projectHelper.addResource(project, protoSourceRoot.getAbsolutePath(),
                ImmutableList.of("**/*.proto"), ImmutableList.of());
    }
}
