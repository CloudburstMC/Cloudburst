package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLeaves;
import org.cloudburstmc.server.block.behavior.BlockBehaviorSolid;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.AIR;
import static org.cloudburstmc.server.block.BlockTypes.FIRE;

/**
 * Created by PetteriM1
 */
public class ItemFireCharge extends Item {

    public ItemFireCharge(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, Vector3f clickPos) {
        if (blockState.getId() == AIR && (target instanceof BlockBehaviorSolid || target instanceof BlockBehaviorLeaves)) {
            BlockBehaviorFire fire = (BlockBehaviorFire) BlockState.get(FIRE);
            fire.setPosition(blockState.getPosition());
            fire.setLevel(level);

            if (fire.isBlockTopFacingSurfaceSolid(fire.down()) || fire.canNeighborBurn()) {
                BlockIgniteEvent e = new BlockIgniteEvent(blockState, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                blockState.getLevel().getServer().getPluginManager().callEvent(e);

                if (!e.isCancelled()) {
                    level.setBlock(fire.getPosition(), fire, true);
                    level.addSound(blockState.getPosition(), Sound.MOB_GHAST_FIREBALL);
                    level.scheduleUpdate(fire, fire.tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                if (player.isSurvival()) {
                    Item item = player.getInventory().getItemInHand();
                    item.setCount(item.getCount() - 1);
                    player.getInventory().setItemInHand(item);
                }
                return true;
            }
        }
        return false;
    }
}
