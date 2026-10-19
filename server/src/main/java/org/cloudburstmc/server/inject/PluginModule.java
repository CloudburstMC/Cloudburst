package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.configuration.ConfigurationLoader;
import org.cloudburstmc.api.plugin.PluginConfiguration;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.configuration.codec.ConfigurationResolvers;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.plugin.CloudPluginConfiguration;
import org.slf4j.Logger;

import java.nio.file.Path;

@RequiredArgsConstructor
public class PluginModule extends AbstractModule {

    private final PluginDescription description;
    private final Logger logger;
    private final Path dataDirectory;
    private final Class<?> pluginClass;

    @Override
    protected void configure() {
        this.bind(this.pluginClass).in(Scopes.SINGLETON);

        this.bind(PluginDescription.class).toInstance(this.description);
        this.bind(Logger.class).toInstance(this.logger);
        this.bind(Path.class).toInstance(this.dataDirectory);

        ConfigurationLoader loader = new YamlConfigurationLoader(new ConfigurationResolvers(id ->
                CloudServer.getInstance().getLevels().stream()
                        .filter(level -> level.getId().equals(id))
                        .findFirst()
                        .orElse(null),
                id -> CloudServer.getInstance().getBlockRegistry().getBlock(id),
                id -> CloudServer.getInstance().getOfflinePlayer(id),
                name -> CloudServer.getInstance().getOfflinePlayer(name)));

        this.bind(ConfigurationLoader.class).toInstance(loader);
        this.bind(PluginConfiguration.class).toInstance(new CloudPluginConfiguration(loader, this.pluginClass, this.dataDirectory));
    }
}
