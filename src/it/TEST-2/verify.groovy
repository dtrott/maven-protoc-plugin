buildLogFile = new File(basedir, 'build.log');
assert buildLogFile.exists();
assert buildLogFile.isFile();

content = buildLogFile.text;
assert content.contains('Toolchain (protobuf) matched:PROTOC');

return true;
