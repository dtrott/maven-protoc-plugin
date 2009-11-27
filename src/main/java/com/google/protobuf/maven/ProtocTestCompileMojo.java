package com.google.protobuf.maven;

import com.google.common.collect.ImmutableList;

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.List;

/**
 *
 * @phase generate-test-sources
 * @goal testCompile
 * @requiresDependencyResolution test
 */
public final class ProtocTestCompileMojo extends AbstractProtocMojo {

  /**
   * The source directories containing the sources to be compiled.
   *
   * @parameter default-value="${basedir}/src/test/proto"
   * @required
   */
  private File protoTestSourceRoot;

  /**
   * This is the directory into which the {@code .java} will be created.
   *
   * @parameter default-value="${project.build.directory}/generated-test-sources/protoc"
   * @required
   */
  private File outputDirectory;

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
  protected File getProtoSourceRoot() {
    return protoTestSourceRoot;
  }

}
