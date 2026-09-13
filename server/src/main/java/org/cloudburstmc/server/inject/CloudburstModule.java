package org.cloudburstmc.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.inject.qualifier.LevelDirectory;

import java.nio.file.Path;

@RequiredArgsConstructor
public class CloudburstModule extends AbstractModule {

    private final CloudServer server;
    private final Path levelPath;

    @Override
    protected void configure() {
        this.bind(Path.class).annotatedWith(LevelDirectory.class).toInstance(this.levelPath);
        this.bind(Thread.class).annotatedWith(Names.named("primary")).toInstance(this.server.getPrimaryThread());
    }
}
