buildLogFile = new File(basedir, 'build.log');
assert buildLogFile.exists();
assert buildLogFile.isFile();

content = buildLogFile.text;
assert content.contains("No 'protocExecutable' parameter is configured, using the default: 'protoc'");

return true;
