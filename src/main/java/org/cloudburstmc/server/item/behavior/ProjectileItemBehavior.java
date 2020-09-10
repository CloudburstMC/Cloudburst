package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.entity.impl.projectile.EntityEnderPearl;
import org.cloudburstmc.server.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;

/**
 * @author CreeperFace
 */
@RequiredArgsConstructor
@Getter
public abstract class ProjectileItemBehavior extends CloudItemBehavior {

    protected final EntityType<? extends Projectile> projectileEntityType;
    protected final float throwForce;

    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        Location location = Location.from(player.getPosition().add(0, player.getEyeHeight() - 0.3f, 0),
                player.getYaw(), player.getPitch(), player.getLevel());

        Projectile projectile = EntityRegistry.get().newEntity(this.getProjectileEntityType(), location);
        projectile.setPosition(location.getPosition());
        projectile.setRotation(player.getYaw(), player.getPitch());
        projectile.setMotion(directionVector);
        projectile.setOwner(player);
        this.onProjectileCreation(item, projectile);

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
                player.getInventory().decrementHandCount();
            }
            if (projectile instanceof EntityEnderPearl) {
                player.onThrowEnderPearl();
            }
            projectile.spawnToAll();
            player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.BOW);
        }
        return true;
    }

    protected void onProjectileCreation(ItemStack item, Projectile projectile) {

    }
}
