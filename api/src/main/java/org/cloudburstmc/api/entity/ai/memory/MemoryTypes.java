package org.cloudburstmc.api.entity.ai.memory;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;

public interface MemoryTypes {

    /**
     * Physical visual target memory
     */
    MemoryType<Vector3f> LOOK_TARGET = new MemoryType<>(Identifier.from("minecraft", "look_target"));

    /**
     * Entity move target position.
     */
    MemoryType<Vector3f> MOVE_TARGET = new MemoryType<>(Identifier.from("minecraft", "move_target"));

    /**
     * Entity movement direction start position.
     */
    MemoryType<Vector3f> MOVE_DIRECTION_START = new MemoryType<>(Identifier.from("minecraft", "move_direction_start"));

    /**
     * Entity movement direction end position.
     */
    MemoryType<Vector3f> MOVE_DIRECTION_END = new MemoryType<>(Identifier.from("minecraft", "move_direction_end"));

    /**
     * Whether the entity needs to update its move direction.
     */
    MemoryType<Boolean> SHOULD_UPDATE_MOVE_DIRECTION =
            new MemoryType<>(Identifier.from("minecraft", "should_update_move_direction"), () -> false);

    /**
     * Runtime ID of the nearest player.
     * Written by {@code NearestPlayerSensor}.
     */
    MemoryType<Long> NEAREST_PLAYER = new MemoryType<>(Identifier.from("minecraft", "nearest_player"));

    /**
     * Unique ID of the nearest player holding a breeding item.
     * Written by {@code NearestFeedingPlayerSensor}.
     */
    MemoryType<Long> NEAREST_FEEDING_PLAYER = new MemoryType<>(Identifier.from("minecraft", "nearest_feeding_player"));

    /**
     * Whether the entity is currently in love mode.
     */
    MemoryType<Boolean> IS_IN_LOVE = new MemoryType<>(Identifier.from("minecraft", "is_in_love"), () -> false);

    /**
     * Runtime ID of this entity's breeding spouse.
     */
    MemoryType<Long> ENTITY_SPOUSE = new MemoryType<>(Identifier.from("minecraft", "entity_spouse"));

    /**
     * The entity tick when this entity last entered love mode.
     */
    MemoryType<Long> LAST_IN_LOVE_TIME = new MemoryType<>(Identifier.from("minecraft", "last_in_love_time"), () -> -1L);

    MemoryType<Long> LAST_BE_FEED_TIME = new MemoryType<>(Identifier.from("minecraft", "last_be_feed_time"), () -> -1L);
}
