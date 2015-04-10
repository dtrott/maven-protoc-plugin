generatedJavaFile = new File(basedir, 'target/classes/a/AaaProtos.class');
assert generatedJavaFile.exists();
assert generatedJavaFile.isFile();

attachedResource = new File(basedir, 'target/classes/a/aaa.proto');
assert attachedResource.exists();
assert attachedResource.isFile();

generatedJavaTestFile = new File(basedir, 'target/test-classes/b/BbbProtos.class');
assert generatedJavaTestFile.exists();
assert generatedJavaTestFile.isFile();

attachedTestResource = new File(basedir, 'target/test-classes/b/bbb.proto');
assert attachedTestResource.exists();
assert attachedTestResource.isFile();

return true;
