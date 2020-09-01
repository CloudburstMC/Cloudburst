package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.plugin.PluginContainer;
import org.cloudburstmc.server.utils.Config;
import org.cloudburstmc.server.utils.ConfigFile;
import org.cloudburstmc.server.utils.PluginConfig;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@RequiredArgsConstructor
public class PluginModule extends AbstractModule {

    private final PluginContainer container;
    private final Class<?> pluginClass;

    @Override
    protected void configure() {
        this.bind(this.pluginClass).in(Scopes.SINGLETON);

        this.bind(PluginContainer.class).toInstance(this.container);
        this.bind(Logger.class).toInstance(this.container.getLogger());

        this.bind(Path.class).annotatedWith(ConfigFile.class).toInstance(container.getDirectory().resolve("config.yml"));
        this.bind(File.class).annotatedWith(ConfigFile.class).toInstance(container.getDirectory().resolve("config.yml").toFile());
        this.bind(Config.class).to(PluginConfig.class);
    }
}
