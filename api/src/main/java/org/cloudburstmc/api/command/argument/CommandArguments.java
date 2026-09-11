package org.cloudburstmc.api.command.argument;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.command.argument.resolver.EntitySelectorResolver;
import org.cloudburstmc.api.command.argument.resolver.PlayerSelectorResolver;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.potion.EffectType;

/**
 * Creates command arguments backed by the server's live registries and entity selector implementation.
 *
 * <p>Obtain this service from {@link org.cloudburstmc.api.command.Commands#arguments()}.</p>
 */
public interface CommandArguments {

    /**
     * Creates an item argument backed by the item registry.
     *
     * @return an item argument
     */
    default CommandArgumentType<ItemType> item() {
        return this.item(null);
    }

    /**
     * Creates an item argument backed by the item registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an item argument
     */
    CommandArgumentType<ItemType> item(@Nullable String displayName);

    /**
     * Creates a block argument backed by the block registry.
     *
     * @return a block argument
     */
    default CommandArgumentType<BlockType> block() {
        return this.block(null);
    }

    /**
     * Creates a block argument backed by the block registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a block argument
     */
    CommandArgumentType<BlockType> block(@Nullable String displayName);

    /**
     * Creates an enchantment argument backed by the enchantment registry.
     *
     * @return an enchantment argument
     */
    default CommandArgumentType<EnchantmentType> enchantment() {
        return this.enchantment(null);
    }

    /**
     * Creates an enchantment argument backed by the enchantment registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an enchantment argument
     */
    CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName);

    /**
     * Creates an effect argument backed by the effect registry.
     *
     * @return an effect argument
     */
    default CommandArgumentType<EffectType> effect() {
        return this.effect(null);
    }

    /**
     * Creates an effect argument backed by the effect registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an effect argument
     */
    CommandArgumentType<EffectType> effect(@Nullable String displayName);

    /**
     * Creates a particle argument backed by the particle registry.
     *
     * @return a particle argument
     */
    default CommandArgumentType<ParticleType> particle() {
        return this.particle(null);
    }

    /**
     * Creates a particle argument backed by the particle registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a particle argument
     */
    CommandArgumentType<ParticleType> particle(@Nullable String displayName);

    /**
     * Creates an argument that resolves one player.
     *
     * @return a single-player selector argument
     */
    default CommandArgumentType<PlayerSelectorResolver> player() {
        return this.player(null);
    }

    /**
     * Creates an argument that resolves one player.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a single-player selector argument
     */
    CommandArgumentType<PlayerSelectorResolver> player(@Nullable String displayName);

    /**
     * Creates an argument that resolves zero or more players.
     *
     * @return a multi-player selector argument
     */
    default CommandArgumentType<PlayerSelectorResolver> players() {
        return this.players(null);
    }

    /**
     * Creates an argument that resolves zero or more players.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a multi-player selector argument
     */
    CommandArgumentType<PlayerSelectorResolver> players(@Nullable String displayName);

    /**
     * Creates an argument that resolves one entity.
     *
     * @return a single-entity selector argument
     */
    default CommandArgumentType<EntitySelectorResolver> entity() {
        return this.entity(null);
    }

    /**
     * Creates an argument that resolves one entity.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a single-entity selector argument
     */
    CommandArgumentType<EntitySelectorResolver> entity(@Nullable String displayName);

    /**
     * Creates an argument that resolves zero or more entities.
     *
     * @return a multi-entity selector argument
     */
    default CommandArgumentType<EntitySelectorResolver> entities() {
        return this.entities(null);
    }

    /**
     * Creates an argument that resolves zero or more entities.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a multi-entity selector argument
     */
    CommandArgumentType<EntitySelectorResolver> entities(@Nullable String displayName);
}
