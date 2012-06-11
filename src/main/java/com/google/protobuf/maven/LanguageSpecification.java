package com.google.protobuf.maven;

import java.io.File;

public class LanguageSpecification {

	private Language language;
	private File outputDirectory;

	public Language getLanguage() {
		return this.language;
	}

	public LanguageSpecification() {

	}

	public LanguageSpecification(Language language, File outputDirectory) {
		this.language = language;
		this.outputDirectory = outputDirectory;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public File getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) || obj.toString().equals(this.language.toString());
	}
}