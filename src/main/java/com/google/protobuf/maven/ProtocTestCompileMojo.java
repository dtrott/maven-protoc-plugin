package com.google.protobuf.maven;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.ImmutableList;

/**
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
	 * @parameter
	 * @required
	 */
	private List<LanguageSpecification> languageSpecifications;

	@Override
	protected void attachFiles(Language lang) throws MojoExecutionException {
		this.project.addTestCompileSourceRoot(this.getOutputDirectory(lang).getAbsolutePath());
		this.projectHelper.addTestResource(this.project, this.protoTestSourceRoot.getAbsolutePath(),
				ImmutableList.of("**/*.proto"), ImmutableList.of());
	}

	@Override
	protected List<Artifact> getDependencyArtifacts() {
		// TODO(gak): maven-project needs generics
		@SuppressWarnings("unchecked")
		List<Artifact> testArtifacts = this.project.getTestArtifacts();
		return testArtifacts;
	}

	@Override
	protected File getOutputDirectory(Language lang) throws MojoExecutionException {

		for (LanguageSpecification langSpec : this.languageSpecifications) {
			if (langSpec.equals(lang)) {
				return langSpec.getOutputDirectory();
			}
		}
		throw new MojoExecutionException("Language specification for " + lang.toString() + "not found.");

	}

	@Override
	protected File getProtoSourceRoot() {
		return this.protoTestSourceRoot;
	}

	@Override
	protected Collection<LanguageSpecification> getLanguages() {
		return this.languageSpecifications;
	}

}
