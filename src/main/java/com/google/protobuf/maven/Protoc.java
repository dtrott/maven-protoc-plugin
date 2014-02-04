package com.google.protobuf.maven;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * This class represents an invokable configuration of the {@code protoc}
 * compiler. The actual executable is invoked using the plexus
 * {@link Commandline}.
 * <p/>
 * 
 * @author gak@google.com (Gregory Kick)
 * @author uismail@ea.com (Usman Ismail)
 */
final class Protoc {
	private final String executable;
	private final ImmutableSet<File> protoPathElements;
	private final ImmutableSet<File> protoFiles;
	private final File outputDirectory;
	private final CommandLineUtils.StringStreamConsumer output;
	private final CommandLineUtils.StringStreamConsumer error;
	private final Language language;

	/**
	 * Constructs a new instance. This should only be used by the
	 * {@link Builder}.
	 * 
	 * @param executable
	 *            The path to the {@code protoc} executable.
	 * @param protoPath
	 *            The directories in which to search for imports.
	 * @param protoFiles
	 *            The proto source files to compile.
	 * @param outputDirectory
	 *            The directory into which the source files will be generated.
	 * @param langSpec
	 *            A specifies the language and the output path for this compile
	 *            session
	 */
	private Protoc(String executable, ImmutableSet<File> protoPath, ImmutableSet<File> protoFiles,
			File outputDirectory, Language language) {
		this.executable = checkNotNull(executable, "executable");
		this.protoPathElements = checkNotNull(protoPath, "protoPath");
		this.protoFiles = checkNotNull(protoFiles, "protoFiles");
		this.outputDirectory = checkNotNull(outputDirectory, "outputDirectory");
		this.language = checkNotNull(language, "language");
		this.error = new CommandLineUtils.StringStreamConsumer();
		this.output = new CommandLineUtils.StringStreamConsumer();

	}

	/**
	 * Invokes the {@code protoc} compiler using the configuration specified at
	 * construction.
	 * 
	 * @return The exit status of {@code protoc}.
	 * @throws CommandLineException
	 */
	public int compile() throws CommandLineException {
		Commandline cl = new Commandline();
		cl.setExecutable(this.executable);
		cl.addArguments(this.buildProtocCommand().toArray(new String[] {}));
		return CommandLineUtils.executeCommandLine(cl, null, this.output, this.error);
	}

	/**
	 * Creates the command line arguments.
	 * <p/>
	 * This method has been made visible for testing only.
	 * 
	 * @return A list consisting of the executable followed by any arguments.
	 */
	ImmutableList<String> buildProtocCommand() {
		final List<String> command = newLinkedList();
		// add the executable
		for (File protoPathElement : this.protoPathElements) {
			command.add("--proto_path=" + protoPathElement);
		}
		command.add(this.language.getCompileCommand() + this.outputDirectory);
		for (File protoFile : this.protoFiles) {
			command.add(protoFile.toString());
		}
		return ImmutableList.copyOf(command);
	}

	/**
	 * @return the output
	 */
	public String getOutput() {
		return this.output.getOutput();
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return this.error.getOutput();
	}

	/**
	 * This class builds {@link Protoc} instances.
	 * 
	 * @author gak@google.com (Gregory Kick)
	 */
	static final class Builder {
		private final String executable;
		private final File outputDirectory;
		private Set<File> protopathElements;
		private Set<File> protoFiles;
		private final Language language;

		/**
		 * Constructs a new builder. The two parameters are present as they are
		 * required for all {@link Protoc} instances.
		 * 
		 * @param executable
		 *            The path to the {@code protoc} executable.
		 * @param outputDirectory
		 *            The directory into which the source files will be
		 *            generated.
		 * @throws NullPointerException
		 *             If either of the arguments are {@code null}.
		 * @throws IllegalArgumentException
		 *             If the {@code outputDirectory} is not a directory.
		 */
		public Builder(String executable, File outputDirectory, Language language) {
			this.executable = checkNotNull(executable, "executable");
			this.outputDirectory = checkNotNull(outputDirectory);
			this.language = checkNotNull(language);
			checkArgument(outputDirectory.isDirectory());
			this.protoFiles = newHashSet();
			this.protopathElements = newHashSet();

		}

		/**
		 * Adds a proto file to be compiled. Proto files must be on the
		 * protopath and this method will fail if a proto file is added without
		 * first adding a parent directory to the protopath.
		 * 
		 * @param protoFile
		 * @return The builder.
		 * @throws IllegalStateException
		 *             If a proto file is added without first adding a parent
		 *             directory to the protopath.
		 * @throws NullPointerException
		 *             If {@code protoFile} is {@code null}.
		 */
		public Builder addProtoFile(File protoFile) {
			checkNotNull(protoFile);
			checkArgument(protoFile.isFile());
			checkArgument(protoFile.getName().endsWith(".proto"));
			this.checkProtoFileIsInProtopath(protoFile);
			this.protoFiles.add(protoFile);
			return this;
		}

		private void checkProtoFileIsInProtopath(File protoFile) {
			assert protoFile.isFile();
			checkState(this.checkProtoFileIsInProtopathHelper(protoFile.getParentFile()));
		}

		private boolean checkProtoFileIsInProtopathHelper(File directory) {
			assert directory.isDirectory();
			if (this.protopathElements.contains(directory)) {
				return true;
			} else {
				final File parentDirectory = directory.getParentFile();
				return (parentDirectory == null) ? false : this.checkProtoFileIsInProtopathHelper(parentDirectory);
			}
		}

		/**
		 * @see #addProtoFile(File)
		 */
		public Builder addProtoFiles(Iterable<File> protoFiles) {
			for (File protoFile : protoFiles) {
				this.addProtoFile(protoFile);
			}
			return this;
		}

		/**
		 * Adds the {@code protopathElement} to the protopath.
		 * 
		 * @param protopathElement
		 *            A directory to be searched for imported protocol buffer
		 *            definitions.
		 * @return The builder.
		 * @throws NullPointerException
		 *             If {@code protopathElement} is {@code null}.
		 * @throws IllegalArgumentException
		 *             If {@code protpathElement} is not a directory.
		 */
		public Builder addProtoPathElement(File protopathElement) {
			checkNotNull(protopathElement);
			checkArgument(protopathElement.isDirectory());
			this.protopathElements.add(protopathElement);
			return this;
		}

		/**
		 * @see #addProtoPathElement(File)
		 */
		public Builder addProtoPathElements(Iterable<File> protopathElements) {
			for (File protopathElement : protopathElements) {
				this.addProtoPathElement(protopathElement);
			}
			return this;
		}

		/**
		 * @return A configured {@link Protoc} instance.
		 * @throws IllegalStateException
		 *             If no proto files have been added.
		 */
		public Protoc build() {
			checkState(!this.protoFiles.isEmpty());
			return new Protoc(this.executable, ImmutableSet.copyOf(this.protopathElements),
					ImmutableSet.copyOf(this.protoFiles), this.outputDirectory, this.language);
		}
	}
}