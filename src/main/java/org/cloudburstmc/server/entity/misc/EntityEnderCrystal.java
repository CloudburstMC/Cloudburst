package org.cloudburstmc.server.entity.misc;

import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.level.Explosion;

/**
 * Created by PetteriM1
 */
public class EntityEnderCrystal extends BaseEntity implements EnderCrystal, Explosive {

    public EntityEnderCrystal(EntityType<EnderCrystal> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    @Override
    public float getHeight() {
        return 0.98f;
    }

    @Override
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source.getCause() == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || source.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            return false;
        }

        if (!super.attack(source)) {
            return false;
        }

        explode();

        return true;
    }

    @Override
    public void explode() {
        Explosion explode = new Explosion(this.getLevel(), this.getPosition(), 6, this);

        this.close();

        if (this.level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            explode.explode();
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public boolean showBase() {
        return this.data.getFlag(EntityFlag.SHOW_BOTTOM);
    }

    public void setShowBase(boolean value) {
        this.data.setFlag(EntityFlag.SHOW_BOTTOM, value);
    }
}
