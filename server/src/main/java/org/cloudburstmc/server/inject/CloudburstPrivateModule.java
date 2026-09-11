package org.cloudburstmc.server.inject;

import com.google.inject.PrivateModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.*;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CloudConsoleCommandSender;
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
        this.bindAndExpose(AsyncScheduler.class).to(CloudAsyncScheduler.class);
        this.bindAndExpose(BiomeRegistry.class).toInstance(CloudBiomeRegistry.get());
        this.bindAndExpose(BlockEntityRegistry.class).toInstance(CloudBlockEntityRegistry.get());
        this.bindAndExpose(BlockRegistry.class).toInstance(CloudBlockRegistry.REGISTRY);
        this.bindAndExpose(CloudBiomeRegistry.class).toInstance(CloudBiomeRegistry.get());
        this.bindAndExpose(CloudBlockRegistry.class).toInstance(CloudBlockRegistry.REGISTRY);
        this.bindAndExpose(CloudCommandRegistry.class).toInstance(this.server.getCommandRegistry());
        this.bindAndExpose(CloudConsoleCommandSender.class);
        this.bindAndExpose(CloudEffectRegistry.class).toInstance(CloudEffectRegistry.get());
        this.bindAndExpose(CloudEnchantmentRegistry.class).toInstance(CloudEnchantmentRegistry.get());
        this.bindAndExpose(CloudEntityRegistry.class).toInstance(CloudEntityRegistry.get());
        this.bindAndExpose(CloudGameRuleRegistry.class).toInstance(CloudGameRuleRegistry.get());
        this.bindAndExpose(CloudItemRegistry.class).toInstance(CloudItemRegistry.get());
        this.bindAndExpose(CloudParticleRegistry.class).toInstance(CloudParticleRegistry.get());
        this.bindAndExpose(CloudRecipeRegistry.class).toInstance(CloudRecipeRegistry.get());
        this.bindAndExpose(CloudServer.class).toInstance(this.server);
        this.bindAndExpose(EffectRegistry.class).toInstance(CloudEffectRegistry.get());
        this.bindAndExpose(EnchantmentRegistry.class).toInstance(CloudEnchantmentRegistry.get());
        this.bindAndExpose(EntityRegistry.class).toInstance(CloudEntityRegistry.get());
        this.bindAndExpose(EventManager.class).to(CloudEventManager.class);
        this.bindAndExpose(GameRuleRegistry.class).toInstance(CloudGameRuleRegistry.get());
        this.bindAndExpose(GeneratorRegistry.class).toInstance(GeneratorRegistry.get());
        this.bindAndExpose(GlobalScheduler.class).to(CloudGlobalScheduler.class);
        this.bindAndExpose(ItemRegistry.class).toInstance(CloudItemRegistry.get());
        this.bindAndExpose(PackManager.class);
        this.bindAndExpose(ParticleRegistry.class).toInstance(CloudParticleRegistry.get());
        this.bindAndExpose(PermissionManager.class).to(CloudPermissionManager.class);
        this.bindAndExpose(PluginManager.class).to(CloudPluginManager.class);
        this.bindAndExpose(RecipeRegistry.class).toInstance(CloudRecipeRegistry.get());
        this.bindAndExpose(ResourcePackRegistry.class).to(PackManager.class);
        this.bindAndExpose(StorageRegistry.class).toInstance(StorageRegistry.get());
    }

    private <T> AnnotatedBindingBuilder<T> bindAndExpose(final Class<T> type) {
        this.expose(type);
        return this.bind(type);
    }
}
