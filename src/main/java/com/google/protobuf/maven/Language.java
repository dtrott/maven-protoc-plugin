package com.google.protobuf.maven;

public enum Language {
	JAVA("--java_out="), CPP("--cpp_out=");

	String compileCommand;

	private Language(String compileCommand) {
		this.compileCommand = compileCommand;
	}

	public String getCompileCommand() {
		return this.compileCommand;
	}

	public void setCompileCommand(String compileCommand) {
		this.compileCommand = compileCommand;
	}
}