package com.google.protobuf.maven.toolchain;

import org.apache.maven.toolchain.Toolchain;

/**
 * A tool chain for protobuf compiler (protoc).
 *
 * @author Sergei Ivanov
 * @since 0.2.0
 */
public interface ProtobufToolchain extends Toolchain {

    String getProtocExecutable();

    void setProtocExecutable(String protocExecutable);
}
