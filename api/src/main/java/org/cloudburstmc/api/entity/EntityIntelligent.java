package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.entity.ai.behaviorgroup.BehaviorGroup;
import org.cloudburstmc.api.entity.ai.memory.MemoryStorage;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.math.vector.Vector3f;

public interface EntityIntelligent extends Living {

    BehaviorGroup getBehaviorGroup();

    default MemoryStorage getMemoryStorage() {
        return getBehaviorGroup().getMemoryStorage();
    }

    default Vector3f getLookTarget() {
        return getMemoryStorage().get(MemoryTypes.LOOK_TARGET);
    }

    default void setLookTarget(Vector3f t) {
        getMemoryStorage().put(MemoryTypes.LOOK_TARGET, t);
    }

    default Vector3f getMoveDirectionStart() {
        return getMemoryStorage().get(MemoryTypes.MOVE_DIRECTION_START);
    }

    default void setMoveDirectionStart(Vector3f v) {
        getMemoryStorage().put(MemoryTypes.MOVE_DIRECTION_START, v);
    }

    default Vector3f getMoveDirectionEnd() {
        return getMemoryStorage().get(MemoryTypes.MOVE_DIRECTION_END);
    }

    default void setMoveDirectionEnd(Vector3f v) {
        getMemoryStorage().put(MemoryTypes.MOVE_DIRECTION_END, v);
    }

    default boolean hasMoveDirection() {
        return getMoveDirectionStart() != null && getMoveDirectionEnd() != null;
    }

    default Vector3f getMoveTarget() {
        return getMemoryStorage().get(MemoryTypes.MOVE_TARGET);
    }

    default void setMoveTarget(Vector3f t) {
        getMemoryStorage().put(MemoryTypes.MOVE_TARGET, t);
    }

    default boolean shouldUpdateMoveDirection() {
        Boolean b = getMemoryStorage().get(MemoryTypes.SHOULD_UPDATE_MOVE_DIRECTION);
        return b != null && b;
    }

    default void setShouldUpdateMoveDirection(boolean v) {
        getMemoryStorage().put(MemoryTypes.SHOULD_UPDATE_MOVE_DIRECTION, v);
    }
}