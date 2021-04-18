package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.inject.provider.config.DataDirProvider;

import java.nio.file.Path;

@RequiredArgsConstructor
public class CloudburstModule extends AbstractModule {

    private final CloudServer server;

    private final Path dataPath;
    private final Path pluginPath;
    private final Path levelPath;

    @Override
    protected void configure() {
        this.bind(Path.class).annotatedWith(DataDirProvider.FILE).toInstance(Bootstrap.PATH);
        this.bind(Path.class).annotatedWith(DataDirProvider.DATA).toInstance(dataPath);
        this.bind(Path.class).annotatedWith(DataDirProvider.PLUGIN).toInstance(pluginPath);
        this.bind(Path.class).annotatedWith(DataDirProvider.LEVEL).toInstance(levelPath);

        this.bind(Thread.class).annotatedWith(Names.named("primary")).toInstance(server.getPrimaryThread());
    }
}
