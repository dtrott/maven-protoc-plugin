package com.google.protobuf.maven;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.List;

/**
 * This mojo executes the {@code protoc} compiler for generating test java sources
 * from protocol buffer definitions. It also searches dependency artifacts in the test scope for
 * {@code .proto} files and includes them in the {@code proto_path} so that they can be
 * referenced. Finally, it adds the {@code .proto} files to the project as test resources so
 * that they can be included in the test-jar artifact.
 *
 * @phase generate-test-sources
 * @goal testCompile
 * @requiresDependencyResolution test
 * @threadSafe
 */
public final class ProtocTestCompileMojo extends AbstractProtocMojo {

    /**
     * The source directories containing the test {@code .proto} definitions to be compiled.
     *
     * @parameter default-value="${basedir}/src/test/proto"
     * @required
     */
    private File protoTestSourceRoot;

    /**
     * This is the directory into which the {@code .java} test sources will be created.
     *
     * @parameter default-value="${project.build.directory}/generated-test-sources/protoc"
     * @required
     */
    private File outputDirectory;

    /**
     * This is the directory into which the (optional) descriptor set file will be created.
     *
     * @parameter default-value="${project.build.directory}/generated-test-sources/protoc-descriptor-sets"
     * @required
     */
    private File descriptorSetOutputDirectory;

    @Override
    protected void addProtocBuilderParameters(final Protoc.Builder protocBuilder) {
        super.addProtocBuilderParameters(protocBuilder);
        // We need to add project output directory to the protobuf import paths,
        // in case test protobuf definitions extend or depend on production ones
        final File buildOutputDirectory = new File(project.getBuild().getOutputDirectory());
        if (buildOutputDirectory.exists()) {
            protocBuilder.addProtoPathElement(buildOutputDirectory);
        }
    }

    @Override
    protected void attachFiles() {
        project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
        projectHelper.addTestResource(project, protoTestSourceRoot.getAbsolutePath(),
                ImmutableList.of("**/*.proto"), ImmutableList.of());
    }

    @Override
    protected List<Artifact> getDependencyArtifacts() {
        // TODO(gak): maven-project needs generics
        @SuppressWarnings("unchecked")
        List<Artifact> testArtifacts = project.getTestArtifacts();
        return testArtifacts;
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
        return protoTestSourceRoot;
    }
}
