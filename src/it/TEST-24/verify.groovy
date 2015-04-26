outputDirectory = new File(basedir, 'project2/target/dependency');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'TEST-24-project1-0.0.1-SNAPSHOT.protobin');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

return true;
