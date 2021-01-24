package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.AreaEffectCloud;
import org.cloudburstmc.server.entity.projectile.LingeringPotion;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.potion.Potion;
import org.cloudburstmc.server.registry.EntityRegistry;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.LINGERING;

public class EntityLingeringPotion extends EntitySplashPotion implements LingeringPotion {

    public EntityLingeringPotion(EntityType<LingeringPotion> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.data.setFlag(LINGERING, true);
    }

    @Override
    protected void splash(Entity collidedWith) {
        super.splash(collidedWith);

        AreaEffectCloud entity = EntityRegistry.get().newEntity(EntityTypes.AREA_EFFECT_CLOUD, this.getLocation());
        entity.setPosition(this.getLocation().getPosition());
        entity.setPotionId(this.getPotionId());

        Effect effect = Potion.getEffect(this.getPotionId(), true);

        if (effect != null) {
            entity.getCloudEffects().add(effect);
            entity.spawnToAll();
        }
    }
}
