package org.cloudburstmc.server.entity.impl.misc;

import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityExplosive;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.misc.EnderCrystal;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.world.Explosion;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.gamerule.GameRules;

/**
 * Created by PetteriM1
 */
public class EntityEnderCrystal extends BaseEntity implements EnderCrystal, EntityExplosive {

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
        Explosion explode = new Explosion(this.getWorld(), this.getPosition(), 6, this);

        this.close();

        if (this.world.getGameRules().get(GameRules.MOB_GRIEFING)) {
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
