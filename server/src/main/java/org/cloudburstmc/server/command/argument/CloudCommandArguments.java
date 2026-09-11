package org.cloudburstmc.server.command.argument;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.resolver.EntitySelectorResolver;
import org.cloudburstmc.api.command.argument.resolver.PlayerSelectorResolver;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.registry.*;

/**
 * Creates command arguments backed by server-owned registries and selector parsing.
 */
public final class CloudCommandArguments implements CommandArguments {
    private final CloudSelectorArguments selectors = new CloudSelectorArguments();

    @Override
    public CommandArgumentType<ItemType> item(@Nullable String displayName) {
        return CommandArgumentTypes.item(displayName, CloudItemRegistry.get());
    }

    @Override
    public CommandArgumentType<BlockType> block(@Nullable String displayName) {
        return CommandArgumentTypes.block(displayName, CloudBlockRegistry.REGISTRY);
    }

    @Override
    public CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName) {
        return CommandArgumentTypes.enchantment(displayName, CloudEnchantmentRegistry.get());
    }

    @Override
    public CommandArgumentType<EffectType> effect(@Nullable String displayName) {
        return CommandArgumentTypes.effect(displayName, CloudEffectRegistry.get());
    }

    @Override
    public CommandArgumentType<ParticleType> particle(@Nullable String displayName) {
        return CommandArgumentTypes.particle(displayName, CloudParticleRegistry.get());
    }

    @Override
    public CommandArgumentType<PlayerSelectorResolver> player(@Nullable String displayName) {
        return this.selectors.player(displayName);
    }

    @Override
    public CommandArgumentType<PlayerSelectorResolver> players(@Nullable String displayName) {
        return this.selectors.players(displayName);
    }

    @Override
    public CommandArgumentType<EntitySelectorResolver> entity(@Nullable String displayName) {
        return this.selectors.entity(displayName);
    }

    @Override
    public CommandArgumentType<EntitySelectorResolver> entities(@Nullable String displayName) {
        return this.selectors.entities(displayName);
    }
}
