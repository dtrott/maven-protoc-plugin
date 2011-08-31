package com.google.protobuf.maven;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.ImmutableList;

/**
 * This mojo executes the {@code protoc} compiler for generating java sources
 * from protocol buffer definitions. It also searches dependency artifacts for
 * proto files and includes them in the protopath so that they can be
 * referenced. Finally, it adds the proto files to the project as resources so
 * that they are included in the final artifact.
 * 
 * @phase generate-sources
 * @goal compile
 * @requiresDependencyResolution compile
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
	 * @parameter
	 * @required
	 */
	private List<LanguageSpecification> languageSpecifications;

	@Override
	protected List<Artifact> getDependencyArtifacts() {
		// TODO(gak): maven-project needs generics
		@SuppressWarnings("unchecked")
		List<Artifact> compileArtifacts = this.project.getCompileArtifacts();
		return compileArtifacts;
	}

	@Override
	protected File getOutputDirectory(Language lang) throws MojoExecutionException {

		for (LanguageSpecification langSpec : this.languageSpecifications) {
			if(langSpec.equals(lang)){
				return langSpec.getOutputDirectory();
			}
		}
		throw new MojoExecutionException("Language specification for " + lang.toString() + "not found.");

	}

	@Override
	protected File getProtoSourceRoot() {
		return this.protoSourceRoot;
	}

	@Override
	protected void attachFiles(Language lang) throws MojoExecutionException {
		this.project.addCompileSourceRoot(this.getOutputDirectory(lang).getAbsolutePath());
		this.projectHelper.addResource(this.project, this.protoSourceRoot.getAbsolutePath(),
				ImmutableList.of("**/*.proto"), ImmutableList.of());
	}

	@Override
	protected Collection<LanguageSpecification> getLanguages() {
		return this.languageSpecifications;
	}
}
