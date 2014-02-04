package com.google.protobuf.maven;

import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * @phase generate-test-sources
 * @goal test-compile-with-dependencies
 * @requiresDependencyResolution test
 */
public class ProtocTestCompileWithDependenciesMojo extends ProtocTestCompileMojo {
	@Override
	protected List<Artifact> getDependencyArtifacts() {
		// TODO(gak): maven-project needs generics
		@SuppressWarnings("unchecked")
		List<Artifact> testArtifacts = this.project.getTestArtifacts();
		return testArtifacts;
	}
}