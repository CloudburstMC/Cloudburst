package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.WindChargeProjectile;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

public class EntityWindChargeProjectile extends EntityAbstractWindCharge implements WindChargeProjectile {

    public EntityWindChargeProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected float burstKnockbackMultiplier() {
        return 1.22f;
    }

    @Override
    protected float burstRadius() {
        return 1.2f;
    }

    @Override
    protected LevelEventType burstParticle() {
        return LevelEvent.PARTICLE_WIND_EXPLOSION;
    }

    @Override
    protected SoundEvent burstSound() {
        return SoundEvent.WIND_CHARGE_BURST;
    }
}
