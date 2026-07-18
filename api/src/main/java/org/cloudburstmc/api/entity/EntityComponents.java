package org.cloudburstmc.api.entity;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.entity.component.BooleanEntityHandler;
import org.cloudburstmc.api.entity.component.FloatEntityHandler;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.entity.component.TickEntityHandler;

/**
 * Standard {@link ComponentType} constants for entity behavioral components.
 * <p>
 * These components allow per-{@link EntityType} customisation of entity behavior
 * without subclassing. Handlers are registered in {@code EntityRegistry} and can
 * be overridden per type.
 */
@UtilityClass
public class EntityComponents {

    /**
     * Returns the base melee attack damage this entity deals.
     */
    public static final ComponentType<FloatEntityHandler> GET_ATTACK_DAMAGE = ComponentType.of("entity_get_attack_damage", FloatEntityHandler.class);

    /**
     * Called when a player interacts with this entity.
     */
    public static final ComponentType<InteractEntityHandler> ON_INTERACT = ComponentType.of("entity_on_interact", InteractEntityHandler.class);

    /**
     * Called on each server tick update for this entity.
     * Returns {@code true} if the entity needs further updates next tick.
     */
    public static final ComponentType<TickEntityHandler> ON_TICK = ComponentType.of("entity_on_tick", TickEntityHandler.class);

    /**
     * Whether this entity type can receive a custom name tag.
     */
    public static final ComponentType<BooleanEntityHandler> CAN_BE_NAMED = ComponentType.of("entity_can_be_named", BooleanEntityHandler.class);

    /**
     * Whether entities of this type can freeze in powder snow.
     */
    public static final ComponentType<BooleanEntityHandler> CAN_FREEZE = ComponentType.of("entity_can_freeze", BooleanEntityHandler.class);

    /**
     * Whether entities of this type can stand on powder snow without leather boots.
     */
    public static final ComponentType<BooleanEntityHandler> CAN_WALK_ON_POWDER_SNOW = ComponentType.of("entity_can_walk_on_powder_snow", BooleanEntityHandler.class);

    /**
     * Returns the multiplier applied to freezing damage for this entity type.
     */
    public static final ComponentType<FloatEntityHandler> GET_FREEZING_DAMAGE_MULTIPLIER = ComponentType.of("entity_get_freezing_damage_multiplier", FloatEntityHandler.class);
}
