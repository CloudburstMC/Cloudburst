package org.cloudburstmc.server.plugin.loader.java;

import org.cloudburstmc.api.plugin.PluginDependency;
import org.cloudburstmc.api.plugin.PluginLoader;
import org.cloudburstmc.server.plugin.CloudPluginDescription;

import java.nio.file.Path;
import java.util.Collection;

public class JavaPluginDescription extends CloudPluginDescription {
    private final String className;

    public JavaPluginDescription(String id, String name, String version, Collection<String> authors, String description,
                                 Collection<PluginDependency> dependencies, String url, Path path, PluginLoader loader,
                                 String className) {
        super(id, name, version, authors, description, dependencies, url, path, loader);
        this.className = className;
    }

    public JavaPluginDescription(JavaPluginDescription description) {
        super(description);
        this.className = description.className;
    }

    public String getClassName() {
        return className;
    }
}
