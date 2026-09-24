package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.BreezeWindChargeProjectile;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

public class EntityBreezeWindChargeProjectile extends EntityAbstractWindCharge implements BreezeWindChargeProjectile {

    public EntityBreezeWindChargeProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected float burstRadius() {
        return 3;
    }

    @Override
    protected LevelEventType burstParticle() {
        return LevelEvent.PARTICLE_BREEZE_WIND_EXPLOSION;
    }

    @Override
    protected SoundEvent burstSound() {
        return SoundEvent.WIND_BURST;
    }
}
