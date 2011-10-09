outputDirectory = new File(basedir, 'target/generated-sources/protoc');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'it/project1/messages/TestProtos1.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package it.project1.messages');
assert content.contains('class TestProtos1');
assert content.contains('class TestMessage1');

outputDirectory = new File(basedir, 'target/generated-test-sources/protoc');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'it/project1/messages/TestProtos2.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package it.project1.messages');
assert content.contains('class TestProtos2');
assert content.contains('class TestMessage2');

return true;
