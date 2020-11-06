package org.cloudburstmc.server.inject;

import com.google.inject.PrivateModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.event.CloudEventManager;
import org.cloudburstmc.server.event.EventManager;
import org.cloudburstmc.server.pack.PackManager;
import org.cloudburstmc.server.permission.CloudPermissionManager;
import org.cloudburstmc.server.permission.PermissionManager;
import org.cloudburstmc.server.plugin.CloudPluginManager;
import org.cloudburstmc.server.registry.*;
import org.cloudburstmc.server.scheduler.ServerScheduler;

@RequiredArgsConstructor
public class CloudburstPrivateModule extends PrivateModule {

    private final CloudServer server;

    @Override
    protected void configure() {
        this.bindAndExpose(CloudServer.class).toInstance(this.server);
        this.bindAndExpose(PluginManager.class).to(CloudPluginManager.class);
        this.bindAndExpose(EventManager.class).to(CloudEventManager.class);
        this.bindAndExpose(PermissionManager.class).to(CloudPermissionManager.class);
        this.bindAndExpose(ServerScheduler.class);
        this.bindAndExpose(PackManager.class);
        this.bindAndExpose(ConsoleCommandSender.class);
//        this.bindAndExpose(ServerScheduler.class);

        this.bind(BiomeRegistry.class).toInstance(BiomeRegistry.get());
        this.bind(BlockEntityRegistry.class).toInstance(BlockEntityRegistry.get());
        this.bind(BlockRegistry.class).toInstance(BlockRegistry.get());
        this.bind(CommandRegistry.class).toInstance(CommandRegistry.get());
        this.bind(EntityRegistry.class).toInstance(EntityRegistry.get());
        this.bind(CloudGameRuleRegistry.class).toInstance(CloudGameRuleRegistry.get());
        this.bind(GeneratorRegistry.class).toInstance(GeneratorRegistry.get());
        this.bind(CloudItemRegistry.class).toInstance(CloudItemRegistry.get());
        this.bind(StorageRegistry.class).toInstance(StorageRegistry.get());
    }

    private <T> AnnotatedBindingBuilder<T> bindAndExpose(final Class<T> type) {
        this.expose(type);
        return this.bind(type);
    }
}
