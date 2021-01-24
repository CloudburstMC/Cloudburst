package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.EnderPearl;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.player.PlayerTeleportEvent;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.player.Player;

public class EntityEnderPearl extends EntityProjectile implements EnderPearl {

    public EntityEnderPearl(EntityType<EnderPearl> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.03f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.isCollided && this.getOwner() instanceof Player) {
            teleport();
        }

        if (this.age > 1200 || this.isCollided) {
            this.kill();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        if (this.getOwner() instanceof Player) {
            teleport();
        }
        super.onCollideWithEntity(entity);
    }

    private void teleport() {
        Entity owner = this.getOwner();
        if (owner != null) {
            owner.teleport(this.getPosition().floor().add(0.5, 0, 0.5), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
            if (((Player) owner).isAdventure() || ((Player) owner).isSurvival()) {
                owner.attack(new EntityDamageByEntityEvent(this, owner, EntityDamageEvent.DamageCause.PROJECTILE, 5f, 0f));
            }
            this.world.addSound(this.getPosition(), Sound.MOB_ENDERMEN_PORTAL);
        }
    }
}
