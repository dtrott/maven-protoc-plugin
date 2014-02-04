package com.google.protobuf.maven;

import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * This mojo executes the {@code protoc} compiler for generating java sources
 * from protocol buffer definitions. It also searches dependency artifacts for
 * proto files and includes them in the protopath so that they can be
 * referenced. Finally, it adds the proto files to the project as resources so
 * that they are included in the final artifact.
 * 
 * @phase generate-sources
 * @goal compile-with-dependencies
 * @requiresDependencyResolution compile
 */

public class ProtocCompileWithDependenciesMojo extends ProtocCompileMojo {
	@Override
	protected List<Artifact> getDependencyArtifacts() {
		// TODO(gak): maven-project needs generics
		@SuppressWarnings("unchecked")
		List<Artifact> compileArtifacts = this.project.getCompileArtifacts();
		return compileArtifacts;
	}
}