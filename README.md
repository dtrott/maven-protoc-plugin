# Maven Protocol Buffers (protoc) Plugin

A plugin that integrates protocol buffers compiler (protoc) into Maven lifecycle.

The latest plugin documentation is available here:
http://sergei-ivanov.github.com/maven-protoc-plugin/

The plugin is not yet available through Maven Central,
for the time being it can be fetched from Bintray:

```xml
<pluginRepositories>
    <pluginRepository>
        <releases>
            <updatePolicy>never</updatePolicy>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>Central Repository</name>
        <url>https://repo.maven.apache.org/maven2</url>
    </pluginRepository>
    <pluginRepository>
        <id>protoc-plugin</id>
        <url>https://dl.bintray.com/sergei-ivanov/maven/</url>
    </pluginRepository>
</pluginRepositories>
```

**NOTE: it is important to include Maven central as the primary plugin repository,
because any custom repository configuration overrides the built-in defaults.**

Alternatively, if Bintray is blocked by firewall rules in your organisation,
try the following repository, hosted on GitHub. Please be aware that GitHub
does not support directory listing, therefore you won't be able to browse
the files, but Maven or Maven repo managers will still be able to fetch the artifacts.

```xml
<pluginRepositories>
    <pluginRepository>
        <releases>
            <updatePolicy>never</updatePolicy>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>Central Repository</name>
        <url>https://repo.maven.apache.org/maven2</url>
    </pluginRepository>
    <pluginRepository>
        <id>protoc-plugin</id>
        <url>http://sergei-ivanov.github.com/maven-protoc-plugin/repo/releases/</url>
    </pluginRepository>
</pluginRepositories>
```
