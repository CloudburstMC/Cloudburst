package org.cloudburstmc.server.dispenser;

import com.nukkitx.nbt.NbtMap;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.ItemStack;

/**
 * @author CreeperFace
 */
public class ProjectileDispenseBehavior implements DispenseBehavior {

    private EntityType<? extends Projectile> entityType;

    public ProjectileDispenseBehavior() {

    }

    public ProjectileDispenseBehavior(EntityType<? extends Projectile> entity) {
        this.entityType = entity;
    }

    @Override
    public void dispense(Block source, ItemStack item) {
//        Location dispensePos = Location.from(source.getDispensePosition(), source.getLevel());
//
//        Direction face = source.getFacing();
//
//        Projectile projectile = EntityRegistry.get().newEntity(entityType, dispensePos);
//        if (projectile == null) {
//            return;
//        }
//
//        projectile.setMotion(Vector3f.from(face.getXOffset(), face.getYOffset() + 0.1f, face.getZOffset()).mul(6));
//        projectile.spawnToAll();
    }

    protected EntityType<?> getEntityType() {
        return this.entityType;
    }

    /**
     * you can add extra data of projectile here
     *
     * @param nbt tag
     */
    protected void correctNBT(NbtMap nbt) {

    }
}
