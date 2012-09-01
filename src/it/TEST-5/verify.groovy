outputDirectory = new File(basedir, 'project1/target/generated-sources/protobuf/java');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'it/project1/messages/TestProtos.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package it.project1.messages');
assert content.contains('class TestProtos');
assert content.contains('class TestMessage1');

outputDirectory = new File(basedir, 'project2/target/generated-sources/protobuf/java');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'it/project2/messages/TestProtos.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package it.project2.messages');
assert content.contains('class TestProtos');
assert content.contains('class TestMessage2');

return true;
