buildLogFile = new File(basedir, 'build.log');
assert buildLogFile.exists();
assert buildLogFile.isFile();

content = buildLogFile.text;
assert content.contains('Found matching toolchain for type protobuf: PROTOC');

return true;
