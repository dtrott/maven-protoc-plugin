package com.google.protobuf.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.Toolchain;

import java.io.File;

/**
 * This mojo executes the {@code protoc} compiler with the specified plugin
 * executable to generate test sources from protocol buffer definitions.
 * It also searches dependency artifacts for {@code .proto} files and
 * includes them in the {@code proto_path} so that they can be referenced.
 * Finally, it adds the {@code .proto} files to the project as resources so
 * that they can be included in the test-jar artifact.
 *
 * @since 0.4.1
 */
@Mojo(
        name = "test-compile-custom",
        defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
)
public final class ProtocTestCompileCustomMojo extends AbstractProtocTestCompileMojo {

    /**
     * A unique id that identifies the plugin to protoc.
     * <strong>Cannot</strong> be one of the built-in protoc plugins:
     * <ul>
     *     <li>java</li>
     *     <li>javanano</li>
     *     <li>cpp</li>
     *     <li>python</li>
     *     <li>descriptor-set</li>
     * </ul>
     */
    @Parameter(
            required = true,
            property = "protocPluginId"
    )
    private String pluginId;

    /**
     * This is the base directory for the generated code.
     * If an explicit {@link #outputDirectory} parameter is not specified,
     * an output directory named after {@link #pluginId} will be created
     * inside this base directory.
     */
    @Parameter(
            required = true,
            readonly = true,
            defaultValue = "${project.build.directory}/generated-test-sources/protobuf"
    )
    private File outputBaseDirectory;

    /**
     * This is the directory where the generated code will be placed.
     * If this parameter is unspecified, then the default location is constructed as follows:<br>
     * {@code ${project.build.directory}/generated-test-sources/protobuf/<pluginId>}
     */
    @Parameter(
            required = false,
            property = "protocPluginOutputDirectory"
    )
    private File outputDirectory;

    /**
     * An optional path to plugin executable.
     * If unspecified, alternative options must be used (e.g. toolchains).
     */
    @Parameter(
            required = false,
            property = "protocPluginExecutable"
    )
    private String pluginExecutable;

    /**
     * An optional parameter to be passed to the plugin.
     * <b>Cannot</b> contain colon (<tt>:</tt>) symbols.
     */
    @Parameter(
            required = false,
            property = "protocPluginParameter"
    )
    private String pluginParameter;

    /**
     * A name of an optional custom toolchain that can be used to locate the plugin executable.
     * The toolchain must be registered as a build extension and initialised properly.
     */
    @Parameter(
            required = false,
            property = "protocPluginToolchain"
    )
    private String pluginToolchain;

    /**
     * If {@link #pluginToolchain} is specified, this parameter specifies the tool in the toolchain,
     * which is to be resolved as plugin executable.
     */
    @Parameter(
            required = false,
            property = "protocPluginTool"
    )
    private String pluginTool;

    /**
     * Plugin artifact specification, in {@code groupId:artifactId:version[:type[:classifier]]} format.
     * When this parameter is set, the specified artifact will be resolved as a plugin executable.
     *
     * @since 0.4.1
     */
    @Parameter(
            required = false,
            property = "protocPluginArtifact"
    )
    private String pluginArtifact;

    @Override
    protected void addProtocBuilderParameters(final Protoc.Builder protocBuilder) throws MojoExecutionException {
        super.addProtocBuilderParameters(protocBuilder);

        protocBuilder.setNativePluginId(pluginId);
        if (pluginToolchain != null && pluginTool != null) {
            //get toolchain from context
            final Toolchain tc = toolchainManager.getToolchainFromBuildContext(pluginToolchain, session);
            if (tc != null) {
                getLog().info("Toolchain in protoc-plugin: " + tc);
                //when the executable to use is explicitly set by user in mojo's parameter, ignore toolchains.
                if (pluginExecutable != null) {
                    getLog().warn("Toolchains are ignored, 'pluginExecutable' parameter is set to " + pluginExecutable);
                } else {
                    //assign the path to executable from toolchains
                    pluginExecutable = tc.findTool(pluginTool);
                }
            }
        }
        if (pluginExecutable == null && pluginArtifact != null) {
            final Artifact artifact = createDependencyArtifact(pluginArtifact);
            final File file = resolveBinaryArtifact(artifact);
            pluginExecutable = file.getAbsolutePath();
        }
        if (pluginExecutable != null) {
            protocBuilder.setNativePluginExecutable(pluginExecutable);
        }
        if (pluginParameter != null) {
            protocBuilder.setNativePluginParameter(pluginParameter);
        }
        protocBuilder.setCustomOutputDirectory(getOutputDirectory());

        // We need to add project output directory to the protobuf import paths,
        // in case test protobuf definitions extend or depend on production ones
        final File buildOutputDirectory = new File(project.getBuild().getOutputDirectory());
        if (buildOutputDirectory.exists()) {
            protocBuilder.addProtoPathElement(buildOutputDirectory);
        }
    }

    @Override
    protected File getOutputDirectory() {
        File outputDirectory = this.outputDirectory;
        if (outputDirectory == null) {
            outputDirectory = new File(outputBaseDirectory, pluginId);
        }
        return outputDirectory;
    }
}
