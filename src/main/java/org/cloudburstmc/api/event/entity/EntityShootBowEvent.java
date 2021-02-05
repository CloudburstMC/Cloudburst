package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityLiving;

/**
 * author: Box
 * Nukkit Project
 */
public class EntityShootBowEvent extends EntityEvent implements Cancellable {

    private final ItemStack bow;

    private Entity projectile;

    private double force;

    public EntityShootBowEvent(EntityLiving shooter, ItemStack bow, Entity projectile, double force) {
        this.entity = shooter;
        this.bow = bow;
        this.projectile = projectile;
        this.force = force;
    }

    @Override
    public EntityLiving getEntity() {
        return (EntityLiving) this.entity;
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
