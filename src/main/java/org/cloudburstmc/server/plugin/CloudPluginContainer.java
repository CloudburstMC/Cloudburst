package org.cloudburstmc.server.plugin;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import org.cloudburstmc.server.inject.PluginModule;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

public class CloudPluginContainer extends CloudPluginDescription implements PluginContainer {

    private Object plugin;
    private final Class<?> pluginClass;
    private final Path pluginDir;
    private final Logger logger;
    private final Injector injector;

    public CloudPluginContainer(Injector injector, Path pluginDir, String id, String name, String version, Collection<String> authors, String description,
                                Collection<PluginDependency> dependencies, String url, Path path, PluginLoader loader, Class<?> pluginClass,
                                Logger logger) {
        super(id, name, version, authors, description, dependencies, url, path, loader);
        this.pluginClass = Objects.requireNonNull(pluginClass, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.pluginDir = pluginDir;
        this.injector = injector.createChildInjector(new PluginModule(this, pluginClass));
    }

    public CloudPluginContainer(Injector injector, Path pluginDir, PluginDescription description, Class<?> pluginClass, Logger logger) {
        super(description);
        this.pluginClass = Objects.requireNonNull(pluginClass, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.pluginDir = pluginDir;
        this.injector = injector.createChildInjector(new PluginModule(this, pluginClass));
    }

    @Nonnull
    @Override
    public Object getPlugin() {
        if (plugin == null) {
            plugin = this.injector.getInstance(this.pluginClass);
        }
        return plugin;
    }

    @Nonnull
    @Override
    public Logger getLogger() {
        return logger;
    }

    @Nonnull
    public Injector getInjector() {
        return injector;
    }

    @Override
    public InputStream getResource(@Nonnull String name) {
        Preconditions.checkNotNull(name, "name");
        return plugin.getClass().getResourceAsStream(name);
    }

    @Override
    public Path getDirectory() {
        return pluginDir;
    }
}
