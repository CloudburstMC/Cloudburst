package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.projectile.EntityEnderPearl;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.EntityRegistry;

/**
 * @author CreeperFace
 */
@RequiredArgsConstructor
@Getter
public class ItemProjectileBehavior extends CloudItemBehavior {

    protected final EntityType<? extends Projectile> projectileEntityType;
    protected final float throwForce;

    public boolean onClickAir(ItemStack item, Vector3f directionVector, CloudPlayer player) {
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
