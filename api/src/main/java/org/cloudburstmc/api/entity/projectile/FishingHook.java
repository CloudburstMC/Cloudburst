package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;

/**
 * A fishing hook cast by a player.
 */
public interface FishingHook extends Projectile {
    /**
     * @return the hook's current movement state
     */
    FishingHookState getFishingState();

    /**
     * @return the hooked entity, or {@code null} when no entity is attached
     */
    @Nullable
    Entity getHookedEntity();

    /**
     * Attaches this hook to an entity, or clears the current attachment.
     *
     * @param entity the entity to attach, or {@code null}
     * @throws IllegalArgumentException if the entity is in another level
     */
    void setHookedEntity(@Nullable Entity entity);

    /**
     * Pulls the attached entity towards the player who cast this hook.
     *
     * @return whether an entity was attached and pulled
     */
    boolean pullHookedEntity();

    /**
     * @return whether the hook currently has a catch ready
     */
    boolean isBiting();

    /**
     * Returns whether the hook meets the open-water requirements for treasure.
     * Open water consists of a 5 by 4 by 5 area containing only source water,
     * air, and lily pads in the required layers.
     *
     * @return whether the hook is fishing in open water
     */
    boolean isOpenWaterFishing();

    /**
     * Attempts to retrieve this hook.
     *
     * <p>This operation fires the relevant fishing event. If the event is canceled,
     * the hook remains active and no durability damage is returned.</p>
     *
     * @param rod the rod used to retrieve the hook
     * @return the durability damage to apply to the rod, or {@code 0} when retrieval was canceled
     * @throws IllegalArgumentException if {@code rod} is not a fishing rod
     * @throws IllegalStateException if this hook has no player owner
     */
    int retrieve(ItemStack rod);
}
