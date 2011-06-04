package com.google.protobuf.maven.toolchain;

import org.apache.maven.toolchain.Toolchain;

public interface ProtobufToolchain extends Toolchain {

    String getProtocExecutable();

    void setProtocExecutable(String mavenHome);
}
