outputDirectory = new File(basedir, 'target/generated-sources/protobuf/cpp');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedHeaderFile = new File(outputDirectory, 'test.pb.h');
assert generatedHeaderFile.exists();
assert generatedHeaderFile.isFile();

content = generatedHeaderFile.text;
assert content.contains('class TestMessage');
assert content.contains('class TestMessage_NestedMessage');
assert content.contains('enum TestMessage_NestedEnum');

generatedClassFile = new File(outputDirectory, 'test.pb.cc');
assert generatedClassFile.exists();
assert generatedClassFile.isFile();

content = generatedClassFile.text;
assert content.contains('TestMessage::TestMessage()');
assert content.contains('TestMessage_NestedMessage::TestMessage_NestedMessage()');

return true;
