package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.entity.Entity;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

public interface PrimedTnt extends Entity {

    @Nullable
    default Entity getSource() {
        return getOwner();
    }

    default void setSource(Entity entity) {
        setOwner(entity);
    }

    @Nonnegative
    int getFuse();

    void setFuse(@Nonnegative int fuse);
}
