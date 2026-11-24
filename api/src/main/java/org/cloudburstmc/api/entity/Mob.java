package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.OffhandView;

/**
 * A living entity with autonomous behavior, such as an animal or a monster.
 */
public interface Mob extends LivingEntity {

    /**
     * Returns the live view of this mob's armor slots.
     */
    ArmorView getArmor();

    /**
     * Returns the live view of this mob's offhand slot.
     */
    OffhandView getOffhand();

    /**
     * Returns the current behavior target, or {@code null} if there is no
     * eligible target. Targets are not saved.
     */
    @Nullable
    LivingEntity getTarget();

    /**
     * Sets the current behavior target, or clears it with {@code null}.
     * Setting a target does not move the mob or attack the target.
     *
     * @throws IllegalArgumentException if the mob cannot target the given entity,
     *                                  such as itself, an entity in another level,
     *                                  or a creative or spectator player
     */
    void setTarget(@Nullable LivingEntity target);
}
