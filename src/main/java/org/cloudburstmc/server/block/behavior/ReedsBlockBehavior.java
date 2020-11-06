package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class ReedsBlockBehavior extends FloodableBlockBehavior {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.REEDS);
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) { //Bonemeal
            int count = 1;
            val level = block.getLevel();

            for (int i = 1; i <= 2; i++) {
                val id = level.getBlockAt(block.getX(), block.getY() - i, block.getZ()).getType();

                if (id == BlockTypes.REEDS) {
                    count++;
                }
            }

            if (count < 3) {
                boolean success = false;
                int toGrow = 3 - count;

                for (int i = 1; i <= toGrow; i++) {
                    Block b = block.up(i);
                    if (b.getState() == BlockStates.AIR) {
                        BlockGrowEvent ev = new BlockGrowEvent(b, BlockState.get(BlockTypes.REEDS));
                        CloudServer.getInstance().getEventManager().fire(ev);

                        if (!ev.isCancelled()) {
                            b.set(ev.getNewState(), true);
                            success = true;
                        }
                    } else if (b.getState().getType() != BlockTypes.REEDS) {
                        break;
                    }
                }

                if (success) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        player.getInventory().decrementHandCount();
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
            if (down.inCategory(BlockCategory.TRANSPARENT) && down.getType() != BlockTypes.REEDS) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (block.downState().getType() != BlockTypes.REEDS) {
                val state = block.getState();

                if (state.ensureTrait(BlockTraits.AGE) == 15) {
                    for (int y = 1; y < 3; ++y) {
                        Block b = block.up(y);
                        if (b.getState() == BlockStates.AIR) {
                            b.set(BlockState.get(BlockTypes.REEDS));
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
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.getState() != BlockStates.AIR) {
            return false;
        }
        Block down = block.down();
        val downType = down.getState().getType();

        if (downType == BlockTypes.REEDS) {
            return placeBlock(block, BlockState.get(BlockTypes.REEDS));
        } else if (downType == BlockTypes.GRASS || downType == BlockTypes.DIRT || downType == BlockTypes.SAND) {
            for (Direction direction : Plane.HORIZONTAL) {
                val sideType = down.getSideState(direction).getType();

                if (sideType == BlockTypes.WATER || sideType == BlockTypes.FLOWING_WATER) {
                    return placeBlock(block, BlockState.get(BlockTypes.REEDS));
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
