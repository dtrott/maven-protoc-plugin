outputDirectory = new File(basedir, 'target/generated-resources/protobuf/descriptor-sets');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'TEST-15-0.0.1-SNAPSHOT.protobin');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

return true;
