package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;

public interface PrimedTnt extends Entity {

    @Nullable
    default Entity getSource() {
        return getOwner();
    }

    default void setSource(Entity entity) {
        setOwner(entity);
    }

    @NonNegative
    int getFuse();

    void setFuse(@NonNegative int fuse);
}
