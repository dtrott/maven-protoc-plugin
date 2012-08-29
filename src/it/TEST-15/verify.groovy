outputDirectory = new File(basedir, 'target/generated-sources/protoc-descriptor-sets');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'descriptorset.protobin');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

return true;
