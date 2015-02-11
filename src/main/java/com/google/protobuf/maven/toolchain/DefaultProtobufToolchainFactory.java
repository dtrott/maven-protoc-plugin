package com.google.protobuf.maven.toolchain;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcher;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * Based on {@code org.apache.maven.toolchain.java.DefaultJavaToolchainFactory}.
 *
 * @author Sergei Ivanov
 * @since 0.2.0
 */
@Component(
        role = ToolchainFactory.class,
        hint = "protobuf",
        description = "A default factory for 'protobuf' toolchains")
public class DefaultProtobufToolchainFactory implements ToolchainFactory {

    @Requirement
    private Logger logger;

    @Override
    public ToolchainPrivate createToolchain(final ToolchainModel model) throws MisconfiguredToolchainException {
        if (model == null) {
            return null;
        }
        final DefaultProtobufToolchain toolchain = new DefaultProtobufToolchain(model, logger);

        // populate the configuration section
        final Properties configuration = toProperties((Xpp3Dom) model.getConfiguration());
        final String protocExecutable = configuration.getProperty(DefaultProtobufToolchain.KEY_PROTOC_EXECUTABLE);
        if (protocExecutable == null) {
            throw new MisconfiguredToolchainException(
                    "Protobuf toolchain without the "
                            + DefaultProtobufToolchain.KEY_PROTOC_EXECUTABLE
                            + " configuration element.");
        }
        final String normalizedProtocExecutablePath = FileUtils.normalize(protocExecutable);
        final File protocExecutableFile = new File(normalizedProtocExecutablePath);
        if (protocExecutableFile.exists()) {
            toolchain.setProtocExecutable(normalizedProtocExecutablePath);
        } else {
            throw new MisconfiguredToolchainException(
                    "Non-existing protoc executable at " + protocExecutableFile.getAbsolutePath());
        }

        // populate the provides section
        final Properties provides = getProvidesProperties(model);
        for (final Map.Entry<Object, Object> provide : provides.entrySet()) {
            final String key = (String) provide.getKey();
            final String value = (String) provide.getValue();

            if (value == null) {
                throw new MisconfiguredToolchainException(
                        "Provides token '" + key + "' doesn't have any value configured.");
            }

            final RequirementMatcher matcher;
            if ("version".equals(key)) {
                matcher = RequirementMatcherFactory.createVersionMatcher(value);
            } else {
                matcher = RequirementMatcherFactory.createExactMatcher(value);
            }
            toolchain.addProvideToken(key, matcher);
        }

        return toolchain;
    }

    @Override
    public ToolchainPrivate createDefaultToolchain() {
        return null;
    }

    /**
     * Get {@code provides} properties in in a way compatible with toolchains descriptor version 1.0
     * (Maven 2.0.9 to 3.2.3, where it is represented as Object/DOM) and descriptor version 1.1
     * (Maven 3.2.4 and later, where it is represented as Properties).
     *
     * @param model the toolchain model as read from XML
     * @return the properties defined in the {@code provides} element
     * @see <a href="http://jira.codehaus.org/browse/MNG-5718">MNG-5718</a>
     */
    protected static Properties getProvidesProperties(final ToolchainModel model) {
        final Object value = getBeanProperty(model, "provides");
        return value instanceof Properties ? (Properties) value : toProperties((Xpp3Dom) value);
    }

    protected static Properties toProperties(final Xpp3Dom dom) {
        final Properties props = new Properties();
        final Xpp3Dom[] children = dom.getChildren();
        for (final Xpp3Dom child : children) {
            props.setProperty(child.getName(), child.getValue());
        }
        return props;
    }

    protected static Object getBeanProperty(final Object obj, final String property) {
        try {
            final Method method = new PropertyDescriptor(property, obj.getClass()).getReadMethod();
            return method.invoke(obj);
        } catch (final IntrospectionException e) {
            throw new RuntimeException("Incompatible toolchain API", e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Incompatible toolchain API", e);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Incompatible toolchain API", e);
        }
    }
}
