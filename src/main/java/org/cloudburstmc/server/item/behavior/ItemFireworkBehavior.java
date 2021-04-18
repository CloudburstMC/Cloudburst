package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.server.level.CloudLevel;
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
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        if (block.getState().getBehavior().canPassThrough(block.getState())) {
            this.spawnFirework(itemStack, (CloudLevel) level, clickPos.toInt().toFloat().add(0.5, 0.5, 0.5));

            if (player.isSurvival()) {
                return itemStack.decrementAmount();
            }
        }

        return null;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        if (player.getInventory().getChestplate().getType() == ItemTypes.ELYTRA && ((CloudPlayer) player).isGliding()) {
            this.spawnFirework(item, (CloudLevel) player.getLevel(), player.getPosition());

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
        rocket.setFireworkData(item.getMetadata(FireworkData.class));

        rocket.spawnToAll();
    }
}
