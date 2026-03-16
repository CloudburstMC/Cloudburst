package org.cloudburstmc.server.inject;

import com.google.inject.PrivateModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.event.CloudEventManager;
import org.cloudburstmc.server.pack.PackManager;
import org.cloudburstmc.server.permission.CloudPermissionManager;
import org.cloudburstmc.server.plugin.CloudPluginManager;
import org.cloudburstmc.server.registry.*;
import org.cloudburstmc.server.scheduler.CloudAsyncScheduler;
import org.cloudburstmc.server.scheduler.CloudGlobalScheduler;

@RequiredArgsConstructor
public class CloudburstPrivateModule extends PrivateModule {

    private final CloudServer server;

    @Override
    protected void configure() {
        this.bindAndExpose(CloudServer.class).toInstance(this.server);
        this.bindAndExpose(PluginManager.class).to(CloudPluginManager.class);
        this.bindAndExpose(EventManager.class).to(CloudEventManager.class);
        this.bindAndExpose(PermissionManager.class).to(CloudPermissionManager.class);
        this.bindAndExpose(GlobalScheduler.class).to(CloudGlobalScheduler.class);
        this.bindAndExpose(AsyncScheduler.class).to(CloudAsyncScheduler.class);
        this.bindAndExpose(PackManager.class);
        this.bindAndExpose(ConsoleCommandSender.class);

        this.bindAndExpose(CloudBiomeRegistry.class).toInstance(CloudBiomeRegistry.get());
        this.bindAndExpose(BlockEntityRegistry.class).toInstance(BlockEntityRegistry.get());
        this.bindAndExpose(CloudBlockRegistry.class).toInstance(CloudBlockRegistry.REGISTRY);
        this.bindAndExpose(CommandRegistry.class).toInstance(CommandRegistry.get());
        this.bindAndExpose(EntityRegistry.class).toInstance(EntityRegistry.get());
        this.bindAndExpose(CloudGameRuleRegistry.class).toInstance(CloudGameRuleRegistry.get());
        this.bindAndExpose(GeneratorRegistry.class).toInstance(GeneratorRegistry.get());
        this.bindAndExpose(CloudItemRegistry.class).toInstance(CloudItemRegistry.get());
        this.bindAndExpose(StorageRegistry.class).toInstance(StorageRegistry.get());
    }

    private <T> AnnotatedBindingBuilder<T> bindAndExpose(final Class<T> type) {
        this.expose(type);
        return this.bind(type);
    }
}