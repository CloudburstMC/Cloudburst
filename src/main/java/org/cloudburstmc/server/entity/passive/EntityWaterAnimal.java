package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityCreature;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityWaterAnimal extends EntityCreature implements EntityAgeable {
    public EntityWaterAnimal(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }
}
