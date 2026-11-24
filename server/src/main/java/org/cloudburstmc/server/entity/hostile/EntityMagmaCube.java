package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.MagmaCube;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityAbstractCubeMob;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

/**
 * @author PikyCZ
 */
public class EntityMagmaCube extends EntityAbstractCubeMob implements MagmaCube {

    public EntityMagmaCube(EntityType<MagmaCube> type, Location location) {
        super(type, location, 4);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
    }

    @Override
    protected String getDefaultName() {
        return "Magma Cube";
    }
}
