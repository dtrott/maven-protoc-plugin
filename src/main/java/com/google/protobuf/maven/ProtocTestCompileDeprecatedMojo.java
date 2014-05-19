package com.google.protobuf.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This mojo is equivalent to {@code test-compile} and is only retained for backwards compatibility.
 *
 * @deprecated please use {@code test-compile} goal instead.
 */
@Deprecated
@Mojo(
        name = "testCompile",
        defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true
)
public final class ProtocTestCompileDeprecatedMojo extends ProtocTestCompileMojo {
    // Only the goal name is different
}
