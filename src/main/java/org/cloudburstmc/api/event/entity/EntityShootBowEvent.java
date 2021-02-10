package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;

/**
 * author: Box
 * Nukkit Project
 */
public final class EntityShootBowEvent extends EntityEvent implements Cancellable {

    private final ItemStack bow;

    private Entity projectile;

    private double force;

    public EntityShootBowEvent(Living shooter, ItemStack bow, Entity projectile, double force) {
        this.entity = shooter;
        this.bow = bow;
        this.projectile = projectile;
        this.force = force;
    }

    @Override
    public Living getEntity() {
        return (Living) this.entity;
    }


    public ItemStack getBow() {
        return this.bow;
    }


    public Entity getProjectile() {
        return this.projectile;
    }

    public void setProjectile(Entity projectile) {
        if (projectile != this.projectile) {
            if (this.projectile.getViewers().size() == 0) {
                this.projectile.kill();
                this.projectile.close();
            }
            this.projectile = projectile;
        }
    }

    public double getForce() {
        return this.force;
    }

    public void setForce(double force) {
        this.force = force;
    }
}
