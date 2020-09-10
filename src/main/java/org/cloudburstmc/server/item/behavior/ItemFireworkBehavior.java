package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.FireworksRocket;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Firework;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;

/**
 * @author CreeperFace
 */
public class ItemFireworkBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        if (block.getState().getBehavior().canPassThrough()) {
            this.spawnFirework(level, block.getPosition().toFloat().add(0.5, 0.5, 0.5));

            if (!player.isCreative()) {
                player.getInventory().decrementCount(player.getInventory().getHeldItemIndex());
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        if (player.getInventory().getChestplate() instanceof ItemElytra && player.isGliding()) {
            this.spawnFirework(player.getLevel(), player.getPosition());

            player.setMotion(Vector3f.from(
                    -Math.sin(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch())) * 2,
                    -Math.sin(Math.toRadians(player.getPitch())) * 2,
                    Math.cos(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch())) * 2));

            if (!player.isCreative()) {
                player.getInventory().decrementHandCount();
            }

            return true;
        }

        return false;
    }

    private void spawnFirework(ItemStack item, Level level, Vector3f pos) {
        FireworksRocket rocket = EntityRegistry.get().newEntity(EntityTypes.FIREWORKS_ROCKET, Location.from(pos, level));
        rocket.setFireworkData(item.ensureMetadata(Firework.class));

        rocket.spawnToAll();
    }
}
