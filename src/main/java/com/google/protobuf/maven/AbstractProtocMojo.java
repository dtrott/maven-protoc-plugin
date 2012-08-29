package com.google.protobuf.maven;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.codehaus.plexus.util.FileUtils.*;

/**
 * Abstract Mojo implementation.
 * <p/>
 * This class is extended by {@link com.google.protobuf.maven.ProtocCompileMojo} and
 * {@link com.google.protobuf.maven.ProtocTestCompileMojo} in order to override the specific configuration for
 * compiling the main or test classes respectively.
 *
 * @author Gregory Kick
 * @author David Trott
 * @author Brice Figureau
 */
abstract class AbstractProtocMojo extends AbstractMojo {

    private static final String PROTO_FILE_SUFFIX = ".proto";

    private static final String DEFAULT_INCLUDES = "**/*" + PROTO_FILE_SUFFIX;

    /**
     * The current Maven project.
     *
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * The current Maven Session Object.
     *
     * @parameter expression="${session}" default-value="${session}"
     * @required
     * @readonly
     * @since 0.2.0
     */
    private MavenSession session;

    /**
     * An optional tool chain manager.
     *
     * @component
     * @since 0.2.0
     */
    private ToolchainManager toolchainManager;

    /**
     * A helper used to add resources to the project.
     *
     * @component
     * @required
     */
    protected MavenProjectHelper projectHelper;

    /**
     * This is the path to the {@code protoc} executable.
     * When this parameter is not set, the plugin attempts to load
     * a {@code protobuf} toolchain and use it locate {@code protoc} executable.
     * If no {@code protobuf} toolchain is defined in the project,
     * the {@code protoc} executable in the {@code PATH} is used.
     *
     * @parameter expression="${protocExecutable}"
     */
    private String protocExecutable;

    /**
     * Additional source paths for {@code .proto} definitions.
     *
     * @parameter
     */
    private File[] additionalProtoPathElements = new File[] {};

    /**
     * Since {@code protoc} cannot access jars, proto files in dependencies are extracted to this location
     * and deleted on exit. This directory is always cleaned during execution.
     *
     * @parameter expression="${project.build.directory}/protoc-dependencies"
     * @required
     */
    private File temporaryProtoFileDirectory;

    /**
     * This is the path to the local maven {@code repository}.
     *
     * @parameter expression="${localRepository}" default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Set this to {@code false} to disable hashing of dependent jar paths.
     * <p/>
     * This plugin expands jars on the classpath looking for embedded {@code .proto} files.
     * Normally these paths are hashed (MD5) to avoid issues with long file names on windows.
     * However if this property is set to {@code false} longer paths will be used.
     *
     * @parameter default-value="true"
     * @required
     */
    private boolean hashDependentPaths;

    /**
     * A list of &lt;include&gt; elements specifying the protobuf definition files (by pattern)
     * that should be included in compilation.
     * When not specified, the default includes will be:
     * <code><br/>
     * &lt;includes&gt;<br/>
     * &nbsp;&lt;include&gt;**&#47;*.proto&lt;/include&gt;<br/>
     * &lt;/includes&gt;<br/>
     * </code>
     *
     * @parameter
     */
    private Set<String> includes = ImmutableSet.of(DEFAULT_INCLUDES);

    /**
     * A list of &lt;exclude&gt; elements specifying the protobuf definition files (by pattern)
     * that should be excluded from compilation.
     * When not specified, the default excludes will be empty:
     * <code><br/>
     * &lt;excludes&gt;<br/>
     * &lt;/excludes&gt;<br/>
     * </code>
     *
     * @parameter
     */
    private Set<String> excludes = ImmutableSet.of();


    /**
     * The descriptor set file name. Only used if {@code writeDescriptorSet} is set to {@code true}.
     * @parameter default-value="descriptorset.protobin"
     * @required
     */
    private String descriptorSetFileName;

    /**
     * If set to {@code true}, the compiler will generate a binary descriptor set file for the
     * specified {@code .proto} files.
     *
     * @parameter default-value="false"
     * @required
     */
    private boolean writeDescriptorSet;

    /**
     * If {@code true} and {@code writeDescriptorSet} has been set, the compiler will include
     * all dependencies in the descriptor set making it "self-contained".
     *
     * @parameter default-value="false"
     */
    private boolean includeDependenciesInDescriptorSet;

    /**
     * Sets the granularity in milliseconds of the last modification date
     * for testing whether source protobuf definitions need recompilation.
     *
     * <p>This parameter is only used when {@link #checkStaleness} parameter is set to {@code true}.
     *
     * <p>If the project is built on NFS it's recommended to set this parameter to {@code 10000}.
     *
     * @parameter default-value="0"
     */
    private long staleMillis;

    /**
     * Normally {@code protoc} is invoked on every execution of the plugin.
     * Setting this parameter to {@code true} will enable checking
     * timestamps of source protobuf definitions vs. generated sources.
     *
     * @parameter default-value="false"
     * @see #staleMillis
     */
    private boolean checkStaleness;

    /**
     * When {@code true}, skip the execution.
     *
     * @parameter expression="${protoc.skip}" default-value="false"
     * @since 0.2.0
     */
    private boolean skip;

    /**
     * Usually most of protobuf mojos will not get executed on parent poms
     * (i.e. projects with packaging type 'pom').
     * Setting this parameter to {@code true} will force
     * the execution of this mojo, even if it would usually get skipped in this case.
     *
     * @parameter expression="${protoc.force}" default-value="false"
     * @required
     * @since 0.2.0
     */
    private boolean forceMojoExecution;

    /**
     * Executes the mojo.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipMojo()) {
            return;
        }

        checkParameters();
        final File protoSourceRoot = getProtoSourceRoot();
        if (protoSourceRoot.exists()) {
            try {
                ImmutableSet<File> protoFiles = findProtoFilesInDirectory(protoSourceRoot);
                final File outputDirectory = getOutputDirectory();
                ImmutableSet<File> outputFiles = findGeneratedFilesInDirectory(getOutputDirectory());

                if (protoFiles.isEmpty()) {
                    getLog().info("No proto files to compile.");
                } else if (checkStaleness && ((lastModified(protoFiles) + staleMillis) < lastModified(outputFiles))) {
                    getLog().info("Skipping compilation because target directory newer than sources.");
                    attachFiles();
                } else {
                    ImmutableSet<File> derivedProtoPathElements =
                            makeProtoPathFromJars(temporaryProtoFileDirectory, getDependencyArtifactFiles());
                    outputDirectory.mkdirs();

                    // Quick fix to fix issues with two mvn installs in a row (ie no clean)
                    cleanDirectory(outputDirectory);

                    if (writeDescriptorSet) {
                        final File descriptorSetOutputDirectory = getDescriptorSetOutputDirectory();
                        descriptorSetOutputDirectory.mkdirs();
                        // See above
                        cleanDirectory(descriptorSetOutputDirectory);
                    }


                    //get toolchain from context
                    Toolchain tc = toolchainManager.getToolchainFromBuildContext("protobuf", session); //NOI18N
                    if (tc != null) {
                        getLog().info("Toolchain in protoc-plugin: " + tc);
                        //when the executable to use is explicitly set by user in mojo's parameter, ignore toolchains.
                        if (protocExecutable != null) {
                            getLog().warn(
                                    "Toolchains are ignored, 'protocExecutable' parameter is set to " + protocExecutable);
                        } else {
                            //assign the path to executable from toolchains
                            protocExecutable = tc.findTool("protoc"); //NOI18N
                        }
                    }
                    if (protocExecutable == null) {
                        // Try to fall back to 'protoc' in $PATH
                        getLog().warn("No 'protocExecutable' parameter is configured, using the default: 'protoc'");
                        protocExecutable = "protoc";
                    }

                    final Protoc.Builder protocBuilder =
                            new Protoc.Builder(protocExecutable, outputDirectory)
                                    .addProtoPathElement(protoSourceRoot)
                                    .addProtoPathElements(derivedProtoPathElements)
                                    .addProtoPathElements(asList(additionalProtoPathElements))
                                    .addProtoFiles(protoFiles);
                    addProtocBuilderParameters(protocBuilder);
                    final Protoc protoc = protocBuilder.build();

                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Proto source root:");
                        getLog().debug(" " + protoSourceRoot);

                        if (derivedProtoPathElements != null && !derivedProtoPathElements.isEmpty()) {
                            getLog().debug("Derived proto paths:");
                            for (final File path : derivedProtoPathElements) {
                                getLog().debug(" " + path);
                            }
                        }

                        if (additionalProtoPathElements != null && additionalProtoPathElements.length > 0) {
                            getLog().debug("Additional proto paths:");
                            for (final File path : additionalProtoPathElements) {
                                getLog().debug(" " + path);
                            }
                        }
                    }
                    protoc.logExecutionParameters(getLog());

                    getLog().info(format("Compiling %d proto file(s) to %s", protoFiles.size(), outputDirectory));

                    final int exitStatus = protoc.compile();
                    if (exitStatus != 0) {
                        getLog().error("protoc failed output: " + protoc.getOutput());
                        getLog().error("protoc failed error: " + protoc.getError());
                        throw new MojoFailureException(
                                "protoc did not exit cleanly. Review output for more information.");
                    }
                    attachFiles();
                }
            } catch (IOException e) {
                throw new MojoExecutionException("An IO error occured", e);
            } catch (IllegalArgumentException e) {
                throw new MojoFailureException("protoc failed to execute because: " + e.getMessage(), e);
            } catch (CommandLineException e) {
                throw new MojoExecutionException("An error occurred while invoking protoc.", e);
            }
        } else {
            getLog().info(format("%s does not exist. Review the configuration or consider disabling the plugin.",
                    protoSourceRoot));
        }
    }

    /**
     * Adds mojo-specific parameters to the protoc builder.
     *
     * @param protocBuilder the builder to be modified.
     */
    protected void addProtocBuilderParameters(final Protoc.Builder protocBuilder) {
        if (writeDescriptorSet) {
            final File descriptorSetFile = new File(getDescriptorSetOutputDirectory(), descriptorSetFileName);
            getLog().info("Will write descriptor set:");
            getLog().info(" " + descriptorSetFile.getAbsolutePath());
            protocBuilder.withDescriptorSetFile(descriptorSetFile, includeDependenciesInDescriptorSet);
        }
    }

    /**
     * <p>Determine if the mojo execution should get skipped.</p>
     * This is the case if:
     * <ul>
     * <li>{@link #skip} is <code>true</code></li>
     * <li>if the mojo gets executed on a project with packaging type 'pom' and
     * {@link #forceMojoExecution} is <code>false</code></li>
     * </ul>
     *
     * @return <code>true</code> if the mojo execution should be skipped.
     * @since 0.2.0
     */
    protected boolean skipMojo() {
        if (skip) {
            getLog().info("Skipping protoc mojo execution");
            return true;
        }

        if (!forceMojoExecution && "pom".equals(this.project.getPackaging())) {
            getLog().info("Skipping protoc mojo execution for project with packaging type 'pom'");
            return true;
        }

        return false;
    }

    protected ImmutableSet<File> findGeneratedFilesInDirectory(File directory) throws IOException {
        if (directory == null || !directory.isDirectory()) {
            return ImmutableSet.of();
        }

        // TODO(gak): plexus-utils needs generics
        @SuppressWarnings("unchecked")
        List<File> javaFilesInDirectory = getFiles(directory, "**/*.java", null);
        return ImmutableSet.copyOf(javaFilesInDirectory);
    }

    private long lastModified(ImmutableSet<File> files) {
        long result = 0;
        for (File file : files) {
            if (file.lastModified() > result) {
                result = file.lastModified();
            }
        }
        return result;
    }

    private void checkParameters() {
        checkNotNull(project, "project");
        checkNotNull(projectHelper, "projectHelper");
        final File protoSourceRoot = getProtoSourceRoot();
        checkNotNull(protoSourceRoot);
        checkArgument(!protoSourceRoot.isFile(), "protoSourceRoot is a file, not a directory");
        checkNotNull(temporaryProtoFileDirectory, "temporaryProtoFileDirectory");
        checkState(!temporaryProtoFileDirectory.isFile(), "temporaryProtoFileDirectory is a file, not a directory");
        final File outputDirectory = getOutputDirectory();
        checkNotNull(outputDirectory);
        checkState(!outputDirectory.isFile(), "the outputDirectory is a file, not a directory");
    }

    protected abstract File getProtoSourceRoot();

    // TODO add artifact filtering (inclusions and exclusions)
    // TODO add filtering for proto definitions in included artifacts
    protected abstract List<Artifact> getDependencyArtifacts();

    /**
     * Returns the output directory for generated sources. Depends on build phase so must
     * be defined in concrete implementation.
     * @return
     */
    protected abstract File getOutputDirectory();

    /**
     * Returns output directory for descriptor set file. Depends on build phase so must
     * be defined in concrete implementation.
     * @return
     */
    protected abstract File getDescriptorSetOutputDirectory();

    protected abstract void attachFiles();

    /**
     * Gets the {@link File} for each dependency artifact.
     *
     * @return A set of all dependency artifacts.
     */
    private ImmutableSet<File> getDependencyArtifactFiles() {
        Set<File> dependencyArtifactFiles = newHashSet();
        for (Artifact artifact : getDependencyArtifacts()) {
            dependencyArtifactFiles.add(artifact.getFile());
        }
        return ImmutableSet.copyOf(dependencyArtifactFiles);
    }

    /**
     * @throws IOException
     */
    protected ImmutableSet<File> makeProtoPathFromJars(
            File temporaryProtoFileDirectory,
            Iterable<File> classpathElementFiles)
            throws IOException, MojoExecutionException {
        checkNotNull(classpathElementFiles, "classpathElementFiles");
        // clean the temporary directory to ensure that stale files aren't used
        if (temporaryProtoFileDirectory.exists()) {
            cleanDirectory(temporaryProtoFileDirectory);
        }
        Set<File> protoDirectories = newHashSet();
        for (File classpathElementFile : classpathElementFiles) {
            // for some reason under IAM, we receive poms as dependent files
            // I am excluding .xml rather than including .jar as there may be other extensions in use (sar, har, zip)
            if (classpathElementFile.isFile() && classpathElementFile.canRead() &&
                    !classpathElementFile.getName().endsWith(".xml")) {

                // create the jar file. the constructor validates.
                JarFile classpathJar;
                try {
                    classpathJar = new JarFile(classpathElementFile);
                } catch (IOException e) {
                    throw new IllegalArgumentException(format(
                            "%s was not a readable artifact", classpathElementFile));
                }
                final Enumeration<JarEntry> jarEntries = classpathJar.entries();
                while (jarEntries.hasMoreElements()) {
                    final JarEntry jarEntry = jarEntries.nextElement();
                    final String jarEntryName = jarEntry.getName();
                    // TODO try using org.codehaus.plexus.util.SelectorUtils.matchPath() with DEFAULT_INCLUDES
                    if (jarEntryName.endsWith(PROTO_FILE_SUFFIX)) {
                        final File jarDirectory =
                                new File(temporaryProtoFileDirectory, truncatePath(classpathJar.getName()));
                        final File uncompressedCopy = new File(jarDirectory, jarEntryName);
                        uncompressedCopy.getParentFile().mkdirs();
                        copyStreamToFile(
                                new RawInputStreamFacade(classpathJar.getInputStream(jarEntry)),
                                uncompressedCopy);
                        protoDirectories.add(jarDirectory);
                    }
                }
            } else if (classpathElementFile.isDirectory()) {
                final List protoFiles = getFiles(classpathElementFile, DEFAULT_INCLUDES, null);
                if (!protoFiles.isEmpty()) {
                    protoDirectories.add(classpathElementFile);
                }
            }
        }
        return ImmutableSet.copyOf(protoDirectories);
    }

    protected ImmutableSet<File> findProtoFilesInDirectory(File directory) throws IOException {
        checkNotNull(directory);
        checkArgument(directory.isDirectory(), "%s is not a directory", directory);
        final Joiner joiner = Joiner.on(',');
        // TODO(gak): plexus-utils needs generics
        @SuppressWarnings("unchecked")
        List<File> protoFilesInDirectory =
                getFiles(directory, joiner.join(includes), joiner.join(excludes));
        return ImmutableSet.copyOf(protoFilesInDirectory);
    }

    protected ImmutableSet<File> findProtoFilesInDirectories(Iterable<File> directories) throws IOException {
        checkNotNull(directories);
        Set<File> protoFiles = newHashSet();
        for (File directory : directories) {
            protoFiles.addAll(findProtoFilesInDirectory(directory));
        }
        return ImmutableSet.copyOf(protoFiles);
    }

    /**
     * Truncates the path of jar files so that they are relative to the local repository.
     *
     * @param jarPath the full path of a jar file.
     * @return the truncated path relative to the local repository or root of the drive.
     */
    protected String truncatePath(final String jarPath) throws MojoExecutionException {

        if (hashDependentPaths) {
            try {
                return toHexString(MessageDigest.getInstance("MD5").digest(jarPath.getBytes()));
            } catch (NoSuchAlgorithmException e) {
                throw new MojoExecutionException("Failed to expand dependent jar", e);
            }
        }

        String repository = localRepository.getBasedir().replace('\\', '/');
        if (!repository.endsWith("/")) {
            repository += "/";
        }

        String path = jarPath.replace('\\', '/');
        int repositoryIndex = path.indexOf(repository);
        if (repositoryIndex != -1) {
            path = path.substring(repositoryIndex + repository.length());
        }

        // By now the path should be good, but do a final check to fix windows machines.
        int colonIndex = path.indexOf(':');
        if (colonIndex != -1) {
            // 2 = :\ in C:\
            path = path.substring(colonIndex + 2);
        }

        return path;
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] byteArray) {
        final StringBuilder hexString = new StringBuilder(2 * byteArray.length);
        for (final byte b : byteArray) {
            hexString.append(HEX_CHARS[(b & 0xF0) >> 4]).append(HEX_CHARS[b & 0x0F]);
        }
        return hexString.toString();
    }
}
