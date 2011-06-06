// A simple test that a custom 'protobuf' tool chain is picked up by maven-toolchains-plugin
// as long as maven-protoc-plugin registers its extensions with maven build.
// No executions for maven-protoc-plugin itself are defined in this pom.

buildLogFile = new File(basedir, 'build.log');
assert buildLogFile.exists();
assert buildLogFile.isFile();

content = buildLogFile.text;
assert content.contains('Toolchain (protobuf) matched:PROTOC');

return true;
