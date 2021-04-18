package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.plugin.PluginDescription;
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
    }
}
