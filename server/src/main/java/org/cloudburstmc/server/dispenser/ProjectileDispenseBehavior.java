package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.nbt.NbtMap;

public class ProjectileDispenseBehavior implements DispenseBehavior {

    private EntityType<? extends Projectile> entityType;

    public ProjectileDispenseBehavior() {

    }

    public ProjectileDispenseBehavior(EntityType<? extends Projectile> entity) {
        this.entityType = entity;
    }

    @Override
    public void dispense(Block source, ItemStack item) {
    }

    protected EntityType<?> getEntityType() {
        return this.entityType;
    }

    protected void correctNBT(NbtMap nbt) {

    }
}
