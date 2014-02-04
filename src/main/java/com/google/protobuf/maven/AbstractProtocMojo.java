package com.google.protobuf.maven;

import static com.google.common.base.Join.join;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static org.codehaus.plexus.util.FileUtils.cleanDirectory;
import static org.codehaus.plexus.util.FileUtils.copyStreamToFile;
import static org.codehaus.plexus.util.FileUtils.getFiles;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import com.google.common.collect.ImmutableSet;

/**
 * Abstract Mojo implementation.
 * <p/>
 * This class is extended by {@link com.google.protobuf.maven.ProtocCompileMojo}
 * and {@link com.google.protobuf.maven.ProtocTestCompileMojo} in order to
 * override the specific configuration for compiling the main or test classes
 * respectively.
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
	 * A helper used to add resources to the project.
	 * 
	 * @component
	 * @required
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 * This is the path to the {@code protoc} executable. By default it will
	 * search the {@code $PATH}.
	 * 
	 * @parameter default-value="protoc"
	 * @required
	 */
	private String protocExecutable;

	/**
	 * @parameter
	 */
	private File[] additionalProtoPathElements = new File[] {};

	/**
	 * Since {@code protoc} cannot access jars, proto files in dependencies are
	 * extracted to this location and deleted on exit. This directory is always
	 * cleaned during execution.
	 * 
	 * @parameter expression="${project.build.directory}/protoc-dependencies"
	 * @required
	 */
	private File temporaryProtoFileDirectory;

	/**
	 * This is the path to the local maven {@code repository}.
	 * 
	 * @parameter default-value="${localRepository}"
	 * @required
	 */
	private ArtifactRepository localRepository;

	/**
	 * Set this to {@code false} to disable hashing of dependent jar paths.
	 * <p/>
	 * This plugin expands jars on the classpath looking for embedded .proto
	 * files. Normally these paths are hashed (MD5) to avoid issues with long
	 * file names on windows. However if this property is set to {@code false}
	 * longer paths will be used.
	 * 
	 * @parameter default-value="true"
	 * @required
	 */
	private boolean hashDependentPaths;

	/**
	 * @parameter
	 */
	private Set<String> includes = ImmutableSet.of(DEFAULT_INCLUDES);

	/**
	 * @parameter
	 */
	private Set<String> excludes = ImmutableSet.of();

	/**
	 * @parameter
	 */
	private long staleMillis = 0;

	/**
	 * @parameter
	 */
	private boolean checkStaleness = false;

	/**
	 * Executes the mojo.
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		this.checkParameters();
		final File protoSourceRoot = this.getProtoSourceRoot();

		if (protoSourceRoot.exists()) {
			for (LanguageSpecification langSpec : this.getLanguages()) {
				this.compileIntoLanguage(protoSourceRoot, langSpec);
			}

		} else {
			this.getLog().info(
					format("%s does not exist. Review the configuration or consider disabling the plugin.",
							protoSourceRoot));
		}
	}

	private void compileIntoLanguage(File protoSourceRoot, LanguageSpecification langSpec)
			throws MojoExecutionException, MojoFailureException {
		try {
			ImmutableSet<File> protoFiles = this.findProtoFilesInDirectory(protoSourceRoot);
			final File outputDirectory = this.getOutputDirectory(langSpec.getLanguage());
			ImmutableSet<File> outputFiles = this.findGeneratedFilesInDirectory(this.getOutputDirectory(langSpec
					.getLanguage()));

			if (protoFiles.isEmpty()) {
				this.getLog().info("No proto files to compile.");
			} else if (this.checkStaleness
					&& ((this.lastModified(protoFiles) + this.staleMillis) < this.lastModified(outputFiles))) {
				this.getLog().info("Skipping compilation because target directory newer than sources.");
				this.attachFiles(langSpec.getLanguage());
			} else {
				ImmutableSet<File> derivedProtoPathElements = this.makeProtoPathFromJars(
						this.temporaryProtoFileDirectory, this.getDependencyArtifactFiles());
				outputDirectory.mkdirs();

				// Quick fix to fix issues with two mvn installs in a row (ie no
				// clean)
				cleanDirectory(outputDirectory);

				Protoc protoc = new Protoc.Builder(this.protocExecutable, outputDirectory, langSpec.getLanguage())
				.addProtoPathElement(protoSourceRoot).addProtoPathElements(derivedProtoPathElements)
				.addProtoPathElements(asList(this.additionalProtoPathElements)).addProtoFiles(protoFiles)
				.build();
				final int exitStatus = protoc.compile();
				if (exitStatus != 0) {
					this.getLog().error("protoc failed output: " + protoc.getOutput());
					this.getLog().error("protoc failed error: " + protoc.getError());
					throw new MojoFailureException("protoc did not exit cleanly. Review output for more information.");
				}
				this.attachFiles(langSpec.getLanguage());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("An IO error occured", e);
		} catch (IllegalArgumentException e) {
			throw new MojoFailureException("protoc failed to execute because: " + e.getMessage(), e);
		} catch (CommandLineException e) {
			throw new MojoExecutionException("An error occurred while invoking protoc.", e);
		}

	}

	ImmutableSet<File> findGeneratedFilesInDirectory(File directory) throws IOException {
		if ((directory == null) || !directory.isDirectory()) {
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

	private void checkParameters() throws MojoExecutionException {
		checkNotNull(this.project, "project");
		checkNotNull(this.projectHelper, "projectHelper");
		checkNotNull(this.protocExecutable, "protocExecutable");
		final File protoSourceRoot = this.getProtoSourceRoot();
		checkNotNull(protoSourceRoot);
		checkArgument(!protoSourceRoot.isFile(), "protoSourceRoot is a file, not a diretory");
		checkNotNull(this.temporaryProtoFileDirectory, "temporaryProtoFileDirectory");
		checkState(!this.temporaryProtoFileDirectory.isFile(), "temporaryProtoFileDirectory is a file, not a directory");

		for (LanguageSpecification langSpec : this.getLanguages()) {
			final File outputDirectory = this.getOutputDirectory(langSpec.getLanguage());
			checkNotNull(outputDirectory);
			checkState(!outputDirectory.isFile(), "the outputDirectory is a file, not a directory");
		}

	}

	protected abstract File getProtoSourceRoot();

	protected abstract List<Artifact> getDependencyArtifacts();

	protected abstract void attachFiles(Language lang) throws MojoExecutionException;

	protected abstract File getOutputDirectory(Language lang) throws MojoExecutionException;

	protected abstract Collection<LanguageSpecification> getLanguages();

	/**
	 * Gets the {@link File} for each dependency artifact.
	 * 
	 * @return A set of all dependency artifacts.
	 */
	private ImmutableSet<File> getDependencyArtifactFiles() {
		Set<File> dependencyArtifactFiles = newHashSet();
		for (Artifact artifact : this.getDependencyArtifacts()) {
			dependencyArtifactFiles.add(artifact.getFile());
		}
		return ImmutableSet.copyOf(dependencyArtifactFiles);
	}

	/**
	 * @throws IOException
	 */
	ImmutableSet<File> makeProtoPathFromJars(File temporaryProtoFileDirectory, Iterable<File> classpathElementFiles)
			throws IOException, MojoExecutionException {
		checkNotNull(classpathElementFiles, "classpathElementFiles");
		// clean the temporary directory to ensure that stale files aren't used
		if (temporaryProtoFileDirectory.exists()) {
			cleanDirectory(temporaryProtoFileDirectory);
		}
		Set<File> protoDirectories = newHashSet();
		for (File classpathElementFile : classpathElementFiles) {
			// for some reason under IAM, we receive poms as dependent files
			// I am excluding .xml rather than including .jar as there may be
			// other extensions in use (sar, har, zip)
			if (classpathElementFile.isFile() && classpathElementFile.canRead()
					&& !classpathElementFile.getName().endsWith(".xml")) {

				// create the jar file. the constructor validates.
				JarFile classpathJar;
				try {
					classpathJar = new JarFile(classpathElementFile);
				} catch (IOException e) {
					throw new IllegalArgumentException(format("%s was not a readable artifact", classpathElementFile));
				}
				for (JarEntry jarEntry : list(classpathJar.entries())) {
					final String jarEntryName = jarEntry.getName();
					if (jarEntry.getName().endsWith(PROTO_FILE_SUFFIX)) {
						final File uncompressedCopy = new File(new File(temporaryProtoFileDirectory,
								this.truncatePath(classpathJar.getName())), jarEntryName);
						uncompressedCopy.getParentFile().mkdirs();
						copyStreamToFile(new RawInputStreamFacade(classpathJar.getInputStream(jarEntry)),
								uncompressedCopy);
						protoDirectories.add(uncompressedCopy.getParentFile());
					}
				}
			} else if (classpathElementFile.isDirectory()) {
				File[] protoFiles = classpathElementFile.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(PROTO_FILE_SUFFIX);
					}
				});

				if (protoFiles.length > 0) {
					protoDirectories.add(classpathElementFile);
				}
			}
		}
		return ImmutableSet.copyOf(protoDirectories);
	}

	ImmutableSet<File> findProtoFilesInDirectory(File directory) throws IOException {
		checkNotNull(directory);
		checkArgument(directory.isDirectory(), "%s is not a directory", directory);
		// TODO(gak): plexus-utils needs generics
		@SuppressWarnings("unchecked")
		List<File> protoFilesInDirectory = getFiles(directory, join(",", this.includes), join(",", this.excludes));
		return ImmutableSet.copyOf(protoFilesInDirectory);
	}

	ImmutableSet<File> findProtoFilesInDirectories(Iterable<File> directories) throws IOException {
		checkNotNull(directories);
		Set<File> protoFiles = newHashSet();
		for (File directory : directories) {
			protoFiles.addAll(this.findProtoFilesInDirectory(directory));
		}
		return ImmutableSet.copyOf(protoFiles);
	}

	/**
	 * Truncates the path of jar files so that they are relative to the local
	 * repository.
	 * 
	 * @param jarPath
	 *            the full path of a jar file.
	 * @return the truncated path relative to the local repository or root of
	 *         the drive.
	 */
	String truncatePath(final String jarPath) throws MojoExecutionException {

		if (this.hashDependentPaths) {
			try {
				return toHexString(MessageDigest.getInstance("MD5").digest(jarPath.getBytes()));
			} catch (NoSuchAlgorithmException e) {
				throw new MojoExecutionException("Failed to expand dependent jar", e);
			}
		}

		String repository = this.localRepository.getBasedir().replace('\\', '/');
		if (!repository.endsWith("/")) {
			repository += "/";
		}

		String path = jarPath.replace('\\', '/');
		int repositoryIndex = path.indexOf(repository);
		if (repositoryIndex != -1) {
			path = path.substring(repositoryIndex + repository.length());
		}

		// By now the path should be good, but do a final check to fix windows
		// machines.
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
