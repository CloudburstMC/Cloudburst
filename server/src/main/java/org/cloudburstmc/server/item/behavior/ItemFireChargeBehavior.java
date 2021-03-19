package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.FIRE;

/**
 * Created by PetteriM1
 */
public class ItemFireChargeBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {

        val targetState = target.getState();

        if (block == BlockStates.AIR && (targetState.getBehavior().isSolid(targetState) || targetState.getType() == BlockTypes.LEAVES)) {
            if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(level.getBlock(clickPos.toInt()).downState()) || BlockBehaviorFire.canNeighborBurn(level.getBlock(clickPos.toInt()))) {
                BlockIgniteEvent e = new BlockIgniteEvent(level.getBlock(clickPos.toInt()), null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                level.getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    val fire = BlockRegistry.get().getBlock(FIRE);
                    level.setBlockState(clickPos.toInt(), fire);

                    ((CloudLevel) level).addSound(clickPos, Sound.MOB_GHAST_FIREBALL);
                    level.scheduleUpdate(clickPos.toInt(), fire.getBehavior().tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                if (player.isSurvival()) {
                    return itemStack.decrementAmount();
                }
            }
        }

        return null;
    }
}
