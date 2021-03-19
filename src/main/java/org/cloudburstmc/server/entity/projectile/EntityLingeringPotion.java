package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.LingeringPotion;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.entity.misc.EntityAreaEffectCloud;
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

        EntityAreaEffectCloud entity = (EntityAreaEffectCloud) EntityRegistry.get().newEntity(EntityTypes.AREA_EFFECT_CLOUD, this.getLocation());
        entity.setPosition(this.getLocation().getPosition());
        entity.setPotionId(this.getPotionId());

        CloudEffect effect = new CloudEffect(EffectType.fromLegacy((byte) this.getPotionId()));

        entity.getCloudEffects().add(effect);
        entity.spawnToAll();
    }
}
