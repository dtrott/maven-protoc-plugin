
def assertGeneratedFile(outputDirectory, fname, content) {
    genFile = new File(outputDirectory, fname)
    assert genFile.exists()
    assert genFile.isFile()
    assert genFile.text == content
}

outputDirectory = new File(basedir, 'target/generated-sources/protobuf/java')
assert outputDirectory.exists()
assert outputDirectory.isDirectory()

assertGeneratedFile(outputDirectory, 'test1.txt', 'test1.proto')
assertGeneratedFile(outputDirectory, 'prefix-test1.txt', 'test1.proto')
assertGeneratedFile(outputDirectory, 'test2.txt', 'test2.proto')
assertGeneratedFile(outputDirectory, 'prefix-test2.txt', 'test2.proto')

return true
