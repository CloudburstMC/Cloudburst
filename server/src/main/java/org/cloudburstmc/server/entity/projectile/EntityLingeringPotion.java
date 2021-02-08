package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.AreaEffectCloud;
import org.cloudburstmc.api.entity.projectile.LingeringPotion;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.server.potion.CloudEffect;
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

        CloudEffect effect = Potion.getEffect(this.getPotionId(), true);

        if (effect != null) {
            entity.getCloudEffects().add(effect);
            entity.spawnToAll();
        }
    }
}
