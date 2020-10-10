package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class ReedsBlockBehavior extends FloodableBlockBehavior {

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.REEDS);
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0F) { //Bonemeal
            int count = 1;
            val level = block.getLevel();

            for (int i = 1; i <= 2; i++) {
                Identifier id = level.getBlockAt(block.getX(), block.getY() - i, block.getZ()).getType();

                if (id == BlockIds.REEDS) {
                    count++;
                }
            }

            if (count < 3) {
                boolean success = false;
                int toGrow = 3 - count;

                for (int i = 1; i <= toGrow; i++) {
                    Block b = block.up(i);
                    if (b.getState() == BlockStates.AIR) {
                        BlockGrowEvent ev = new BlockGrowEvent(b, BlockState.get(BlockIds.REEDS));
                        CloudServer.getInstance().getEventManager().fire(ev);

                        if (!ev.isCancelled()) {
                            b.set(ev.getNewState(), true);
                            success = true;
                        }
                    } else if (b.getState().getType() != BlockIds.REEDS) {
                        break;
                    }
                }

                if (success) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }

                    level.addParticle(new BoneMealParticle(block.getPosition()));
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            BlockState down = block.downState();
            if (down.inCategory(BlockCategory.TRANSPARENT) && down.getType() != BlockIds.REEDS) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (block.downState().getType() != BlockIds.REEDS) {
                val state = block.getState();

                if (state.ensureTrait(BlockTraits.AGE) == 15) {
                    for (int y = 1; y < 3; ++y) {
                        Block b = block.up(y);
                        if (b.getState() == BlockStates.AIR) {
                            b.set(BlockState.get(BlockIds.REEDS));
                            break;
                        }
                    }

                    block.set(state.withTrait(BlockTraits.AGE, 0));
                } else {
                    block.set(state.incrementTrait(BlockTraits.AGE));
                }
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.getState() != BlockStates.AIR) {
            return false;
        }
        Block down = block.down();
        val downType = down.getState().getType();

        if (downType == BlockIds.REEDS) {
            return placeBlock(block, BlockState.get(BlockIds.REEDS));
        } else if (downType == BlockIds.GRASS || downType == BlockIds.DIRT || downType == BlockIds.SAND) {
            for (Direction direction : Plane.HORIZONTAL) {
                val sideType = down.getSideState(direction).getType();

                if (sideType == BlockIds.WATER || sideType == BlockIds.FLOWING_WATER) {
                    return placeBlock(block, BlockState.get(BlockIds.REEDS));
                }
            }
        }
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
