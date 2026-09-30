package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.EquipmentSlot;

import static java.util.Objects.requireNonNull;

/**
 * Fired before lethal damage is prevented. Starts canceled when no death-protection
 * item is held. Uncanceling allows recovery without consuming an item.
 */
public class EntityResurrectEvent extends EntityEvent implements Cancellable {

    private final @Nullable EquipmentSlot hand;

    /**
     * @param entity the entity receiving lethal damage
     * @param hand   the hand holding death protection, or {@code null} when none is held
     * @throws IllegalArgumentException if the slot is not a hand
     */
    public EntityResurrectEvent(Living entity, @Nullable EquipmentSlot hand) {
        this.entity = requireNonNull(entity, "entity");
        if (hand != null && hand != EquipmentSlot.MAIN_HAND && hand != EquipmentSlot.OFF_HAND) {
            throw new IllegalArgumentException("Death protection must be held in a hand");
        }
        this.hand = hand;
        this.setCancelled(hand == null);
    }

    /**
     * @return the entity receiving lethal damage
     */
    @Override
    public Living getEntity() {
        return (Living) this.entity;
    }

    /**
     * @return the hand holding death protection, or {@code null} when none is held
     */
    public @Nullable EquipmentSlot getHand() {
        return this.hand;
    }
}
