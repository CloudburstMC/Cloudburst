package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.event.block.LeavesDecayEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.api.item.ItemTypes.APPLE;
import static org.cloudburstmc.api.item.ItemTypes.STICK;

public class BlockBehaviorLeaves extends BlockBehaviorTransparent {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, BlockRegistry.get().getBlock(LEAVES).withTrait(BlockTraits.IS_PERSISTENT, true));
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(BlockRegistry.get().getBlock(LEAVES).withTrait(BlockTraits.TREE_SPECIES_OVERWORLD, block.getState().ensureTrait(BlockTraits.TREE_SPECIES_OVERWORLD)));
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        val state = block.getState();
        if (hand.getBehavior().isShears()) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            if (this.canDropApple(state) && ThreadLocalRandom.current().nextInt(200) == 0) {
                return new ItemStack[]{
                        CloudItemRegistry.get().getItem(APPLE)
                };
            }
            if (ThreadLocalRandom.current().nextInt(20) == 0) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    return new ItemStack[]{
                            CloudItemRegistry.get().getItem(STICK, ThreadLocalRandom.current().nextInt(1, 2))
                    };
                } else if (state.ensureTrait(BlockTraits.TREE_SPECIES_OVERWORLD) != TreeSpecies.JUNGLE || ThreadLocalRandom.current().nextInt(20) == 0) {
                    return new ItemStack[]{
                            this.getSapling(state)
                    };
                }
            }
        }
        return new ItemStack[0];
    }

    @Override
    public int onUpdate(Block block, int type) {
        val state = block.getState();
        if (type == CloudLevel.BLOCK_UPDATE_RANDOM && !isPersistent(state) && !isCheckDecay(state)) {
            block.set(block.getState().withTrait(BlockTraits.HAS_UPDATE, true));
        } else if (type == CloudLevel.BLOCK_UPDATE_RANDOM && isCheckDecay(state) && !isPersistent(state)) {
            LeavesDecayEvent ev = new LeavesDecayEvent(block);

            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled() || findLog(block, 7)) {
                block.set(state.withTrait(BlockTraits.HAS_UPDATE, false), false, false);
            } else {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    private Boolean findLog(Block pos, Integer distance) {
        for (Block collisionBlock : pos.getLevel().getCollisionBlocks(new SimpleAxisAlignedBB(
                pos.getX() - distance, pos.getY() - distance, pos.getZ() - distance,
                pos.getX() + distance, pos.getY() + distance, pos.getZ() + distance))) {

            val state = collisionBlock.getState().getType();

            if (state == LOG) {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckDecay(BlockState state) {
        return state.ensureTrait(BlockTraits.HAS_UPDATE);
    }

    public boolean isPersistent(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_PERSISTENT);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }


    protected boolean canDropApple(BlockState state) {
        val type = state.ensureTrait(BlockTraits.TREE_SPECIES_OVERWORLD);
        return type == TreeSpecies.OAK || type == TreeSpecies.DARK_OAK;
    }

    protected ItemStack getSapling(BlockState state) {
        return CloudItemRegistry.get().getItem(BlockRegistry.get().getBlock(SAPLING).withTrait(BlockTraits.TREE_SPECIES_OVERWORLD, state.ensureTrait(BlockTraits.TREE_SPECIES_OVERWORLD)));
    }


}
