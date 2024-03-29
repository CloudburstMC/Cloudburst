package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.EnderPearl;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;

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

        if (this.isCollided && this.getOwner() instanceof CloudPlayer) {
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
        if (this.getOwner() instanceof CloudPlayer) {
            teleport();
        }
        super.onCollideWithEntity(entity);
    }

    private void teleport() {
        Entity owner = this.getOwner();
        if (owner != null && owner.getLevel().getId().equals(this.getLevel().getId())) {
            owner.teleport(this.getPosition().floor().add(0.5, 0, 0.5), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
            if (((CloudPlayer) owner).isAdventure() || ((CloudPlayer) owner).isSurvival()) {
                owner.attack(new EntityDamageByEntityEvent(this, owner, EntityDamageEvent.DamageCause.PROJECTILE, 5f, 0f));
            }
            this.level.addSound(this.getPosition(), Sound.MOB_ENDERMEN_PORTAL);
        }
    }
}
