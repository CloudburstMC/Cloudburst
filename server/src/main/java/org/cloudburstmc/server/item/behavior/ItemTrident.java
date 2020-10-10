package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.impl.projectile.EntityProjectile;
import org.cloudburstmc.server.entity.projectile.ThrownTrident;
import org.cloudburstmc.server.event.entity.EntityShootBowEvent;
import org.cloudburstmc.server.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by PetteriM1
 */
public class ItemTrident extends ItemTool {

    public ItemTrident(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_TRIDENT;
    }

    @Override
    public boolean isSword() {
        return true;
    }

    @Override
    public int getAttackDamage() {
        return 9;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return true;
    }

    @Override
    public boolean onRelease(Player player, int ticksUsed) {
        this.useOn(player);

        Vector3f motion = Vector3f.from(
                -Math.sin(player.getYaw() / 180 * Math.PI) * Math.cos(player.getPitch() / 180 * Math.PI),
                -Math.sin(player.getPitch() / 180 * Math.PI),
                Math.cos(player.getYaw() / 180 * Math.PI) * Math.cos(player.getPitch() / 180 * Math.PI)
        );

        double p = (double) ticksUsed / 20;

        double f = Math.min((p * p + p * 2) / 3, 1) * 2;
        Location location = Location.from(player.getPosition().add(0, player.getEyeHeight(), 0),
                (player.getYaw() > 180 ? 360 : 0) - player.getYaw(), -player.getPitch(), player.getLevel());
        ThrownTrident trident = EntityRegistry.get().newEntity(EntityTypes.THROWN_TRIDENT, location);
        trident.setShooter(player);
        trident.setCritical(f == 2);
        trident.setMotion(motion);
        trident.setTrident(this);

        EntityShootBowEvent entityShootBowEvent = new EntityShootBowEvent(player, this, trident, f);

        if (f < 0.1 || ticksUsed < 5) {
            entityShootBowEvent.setCancelled();
        }

        CloudServer.getInstance().getEventManager().fire(entityShootBowEvent);
        if (entityShootBowEvent.isCancelled()) {
            entityShootBowEvent.getProjectile().kill();
        } else {
            entityShootBowEvent.getProjectile().setMotion(entityShootBowEvent.getProjectile().getMotion().mul(entityShootBowEvent.getForce()));
            if (entityShootBowEvent.getProjectile() instanceof EntityProjectile) {
                ProjectileLaunchEvent ev = new ProjectileLaunchEvent(entityShootBowEvent.getProjectile());
                CloudServer.getInstance().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    entityShootBowEvent.getProjectile().kill();
                } else {
                    entityShootBowEvent.getProjectile().spawnToAll();
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ITEM_TRIDENT_THROW);
                    if (!player.isCreative()) {
                        this.decrementCount();
                        player.getInventory().setItemInHand(this);
                    }
                }
            }
        }

        return true;
    }
}
