package com.google.protobuf.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * This mojo executes the {@code protoc} compiler for generating main JavaNano sources
 * from protocol buffer definitions. It also searches dependency artifacts for
 * {@code .proto} files and includes them in the {@code proto_path} so that they can be
 * referenced. Finally, it adds the {@code .proto} files to the project as resources so
 * that they are included in the final artifact.
 *
 * @since 0.4.3
 */
@Mojo(
        name = "compile-javanano",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true
)
public final class ProtocCompileJavaNanoMojo extends AbstractProtocCompileMojo {

    /**
     * This is the directory into which the {@code .java} will be created.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/protobuf/javanano"
    )
    private File outputDirectory;

    /**
     * Optional comma-separated options to be passed to the JavaNano generator.
     * <b>Cannot</b> contain colon (<tt>:</tt>) symbols.
     */
    @Parameter(
            required = false,
            property = "javaNanoOptions"
    )
    private String javaNanoOptions;

    @Override
    protected void addProtocBuilderParameters(final Protoc.Builder protocBuilder) throws MojoExecutionException {
        super.addProtocBuilderParameters(protocBuilder);
        if (javaNanoOptions != null) {
            protocBuilder.setNativePluginParameter(javaNanoOptions);
        }
        protocBuilder.setJavaNanoOutputDirectory(getOutputDirectory());
    }

    @Override
    protected File getOutputDirectory() {
        return outputDirectory;
    }
}
