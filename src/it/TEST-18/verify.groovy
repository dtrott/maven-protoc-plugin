outputDirectory = new File(basedir, 'target/generated-sources/protobuf/python');
assert outputDirectory.exists();
assert outputDirectory.isDirectory();

generatedPythonFile = new File(outputDirectory, 'test_pb2.py');
assert generatedPythonFile.exists();
assert generatedPythonFile.isFile();

content = generatedPythonFile.text;
assert content.contains('class TestMessage');
assert content.contains('class NestedMessage');

return true;
