package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Renders fragments of an item, including its variant and damage.
 *
 * @param item the non-empty item supplying the texture
 */
public record ItemParticleOptions(ItemStack item) implements ParticleOptions {

    public ItemParticleOptions {
        Objects.requireNonNull(item, "item");
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Item particles require a non-empty item");
        }
    }

    @Override
    public ParticleType getType() {
        return ParticleTypes.ICON_CRACK;
    }
}
