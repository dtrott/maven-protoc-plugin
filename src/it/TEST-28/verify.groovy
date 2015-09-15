outputDirectory = new File(basedir, 'target/generated-sources/protobuf/java');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'test/TestProtos.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package test');
assert content.contains('class TestProtos');
assert content.contains('class TestMessage');

outputDirectory = new File(basedir, 'target/generated-sources/protobuf/grpc');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedJavaFile = new File(outputDirectory, 'test/MyServiceGrpc.java');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

content = generatedJavaFile.text;
assert content.contains('package test');
assert content.contains('class MyServiceGrpc');
assert content.contains('interface MyService');


return true;
