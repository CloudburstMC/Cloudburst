package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.level.Location;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.BABY;

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
