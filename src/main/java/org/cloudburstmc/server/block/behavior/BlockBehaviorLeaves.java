package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.LeavesDecayEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.*;
import static org.cloudburstmc.server.item.behavior.ItemIds.APPLE;
import static org.cloudburstmc.server.item.behavior.ItemIds.STICK;

public class BlockBehaviorLeaves extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHEARS;
    }

    @Override
    public int getBurnChance() {
        return 30;
    }

    @Override
    public int getBurnAbility() {
        return 60;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, BlockState.get(LEAVES).withTrait(BlockTraits.IS_PERSISTENT, true));
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(BlockState.get(LEAVES).copyTrait(BlockTraits.TREE_SPECIES, block.getState()));
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        val state = block.getState();
        if (hand.isShears()) {
            return new Item[]{
                    toItem(block)
            };
        } else {
            if (this.canDropApple(state) && ThreadLocalRandom.current().nextInt(200) == 0) {
                return new Item[]{
                        Item.get(APPLE)
                };
            }
            if (ThreadLocalRandom.current().nextInt(20) == 0) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    return new Item[]{
                            Item.get(STICK, 0, ThreadLocalRandom.current().nextInt(1, 2))
                    };
                } else if (state.ensureTrait(BlockTraits.TREE_SPECIES) != TreeSpecies.JUNGLE || ThreadLocalRandom.current().nextInt(20) == 0) {
                    return new Item[]{
                            this.getSapling(state)
                    };
                }
            }
        }
        return new Item[0];
    }

    @Override
    public int onUpdate(Block block, int type) {
        val state = block.getState();
        if (type == World.BLOCK_UPDATE_RANDOM && !isPersistent(state) && !isCheckDecay(state)) {
            block.set(block.getState().withTrait(BlockTraits.HAS_UPDATE, true));
        } else if (type == World.BLOCK_UPDATE_RANDOM && isCheckDecay(state) && !isPersistent(state)) {
            LeavesDecayEvent ev = new LeavesDecayEvent(block);

            Server.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled() || findLog(block, 7)) {
                block.set(state.withTrait(BlockTraits.HAS_UPDATE, false), false, false);
            } else {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    private Boolean findLog(Block pos, Integer distance) {
        for (Block collisionBlock : pos.getWorld().getCollisionBlocks(new SimpleAxisAlignedBB(
                pos.getX() - distance, pos.getY() - distance, pos.getZ() - distance,
                pos.getX() + distance, pos.getY() + distance, pos.getZ() + distance))) {

            val state = collisionBlock.getState().getType();

            if (state == LOG || state == LOG2) {
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

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    protected boolean canDropApple(BlockState state) {
        val type = state.ensureTrait(BlockTraits.TREE_SPECIES);
        return type == TreeSpecies.OAK || type == TreeSpecies.DARK_OAK;
    }

    protected Item getSapling(BlockState state) {
        return Item.get(BlockState.get(SAPLING).copyTrait(BlockTraits.TREE_SPECIES, state));
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
