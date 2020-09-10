package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLeaves;
import org.cloudburstmc.server.block.behavior.BlockBehaviorSolid;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.FIRE;

/**
 * Created by PetteriM1
 */
public class ItemFireChargeBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        if (block.getState() == BlockStates.AIR && (target instanceof BlockBehaviorSolid || target instanceof BlockBehaviorLeaves)) {
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
                    player.getInventory().decrementHandCount();
                }
                return true;
            }
        }
        return false;
    }
}
