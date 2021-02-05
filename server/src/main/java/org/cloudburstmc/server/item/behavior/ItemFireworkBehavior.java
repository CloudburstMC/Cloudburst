package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.data.Firework;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;
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
    public ItemStack onActivate(ItemStack itemStack, CloudPlayer player, Block block, Block target, Direction face, Vector3f clickPos, CloudLevel level) {
        if (block.getState().getBehavior().canPassThrough(block.getState())) {
            this.spawnFirework(itemStack, level, block.getPosition().toFloat().add(0.5, 0.5, 0.5));

            if (player.isSurvival()) {
                return itemStack.decrementAmount();
            }
        }

        return null;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, CloudPlayer player) {
        if (player.getInventory().getChestplate().getType() == ItemTypes.ELYTRA && player.isGliding()) {
            this.spawnFirework(item, player.getLevel(), player.getPosition());

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

    private void spawnFirework(ItemStack item, CloudLevel level, Vector3f pos) {
        FireworksRocket rocket = EntityRegistry.get().newEntity(EntityTypes.FIREWORKS_ROCKET, Location.from(pos, level));
        rocket.setFireworkData(item.getMetadata(Firework.class));

        rocket.spawnToAll();
    }
}
