package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.entity.impl.projectile.EntityEnderPearl;
import org.cloudburstmc.server.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author CreeperFace
 */
public abstract class ProjectileItem extends Item {

    public ProjectileItem(Identifier id) {
        super(id);
    }

    abstract public EntityType<? extends Projectile> getProjectileEntityType();

    abstract public float getThrowForce();

    public boolean onClickAir(Player player, Vector3f directionVector) {
        Location location = Location.from(player.getPosition().add(0, player.getEyeHeight() - 0.3f, 0),
                player.getYaw(), player.getPitch(), player.getWorld());

        Projectile projectile = EntityRegistry.get().newEntity(this.getProjectileEntityType(), location);
        projectile.setPosition(location.getPosition());
        projectile.setRotation(player.getYaw(), player.getPitch());
        projectile.setMotion(directionVector);
        projectile.setOwner(player);
        this.onProjectileCreation(projectile);

        if (projectile instanceof EntityEnderPearl) {
            if (player.getServer().getTick() - player.getLastEnderPearlThrowingTick() < 20) {
                projectile.kill();
                return false;
            }
        }

        projectile.setMotion(projectile.getMotion().mul(this.getThrowForce()));

        ProjectileLaunchEvent ev = new ProjectileLaunchEvent(projectile);

        player.getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            projectile.kill();
        } else {
            if (!player.isCreative()) {
                this.decrementCount();
            }
            if (projectile instanceof EntityEnderPearl) {
                player.onThrowEnderPearl();
            }
            projectile.spawnToAll();
            player.getWorld().addLevelSoundEvent(player.getPosition(), SoundEvent.BOW);
        }
        return true;
    }

    protected void onProjectileCreation(Projectile projectile) {

    }
}
