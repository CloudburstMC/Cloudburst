package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;

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
    public ItemStack onActivate(ItemStack itemStack, CloudPlayer player, Block block, Block target, Direction face, Vector3f clickPos, CloudLevel level) {
        val targetState = target.getState();
        if (block.getState() == BlockStates.AIR && (targetState.getBehavior().isSolid(targetState) || targetState.getType() == BlockTypes.LEAVES)) {
            if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.downState()) || BlockBehaviorFire.canNeighborBurn(block)) {
                BlockIgniteEvent e = new BlockIgniteEvent(block, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                block.getLevel().getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    val fire = BlockState.get(FIRE);
                    block.set(fire);

                    level.addSound(block.getPosition(), Sound.MOB_GHAST_FIREBALL);
                    level.scheduleUpdate(block.getPosition(), fire.getBehavior().tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                if (player.isSurvival()) {
                    return itemStack.decrementAmount();
                }
            }
        }

        return null;
    }
}
