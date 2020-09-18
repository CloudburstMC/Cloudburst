package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;
import org.cloudburstmc.server.utils.data.TallGrassType;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemTypes.DYE;

public class BlockBehaviorTallGrass extends FloodableBlockBehavior {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean canBeReplaced(Block block) {
        return true;
    }

    @Override
    public int getBurnChance() {
        return 60;
    }

    @Override
    public int getBurnAbility() {
        return 100;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        // Prevents from placing the same plant block on itself
        val itemBlock = item.getBehavior().getBlock(item);
        if (itemBlock.getType() == target.getState().getType() && itemBlock.ensureTrait(BlockTraits.TALL_GRASS_TYPE) == block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE)) {
            return false;
        }
        val down = block.down().getState().getType();
        if (down == GRASS || down == DIRT || down == PODZOL) {
            placeBlock(block, itemBlock);
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) {
            val up = block.up();

            if (up.getState().getType() == AIR) {
                val type = block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE);
                if (type == TallGrassType.DEFAULT) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        player.getInventory().decrementHandCount();
                    }

                    block.getLevel().addParticle(new BoneMealParticle(block.getPosition()));

                    val dp = BlockState.get(DOUBLE_PLANT).withTrait(BlockTraits.TALL_GRASS_TYPE, type);
                    block.set(dp, true);
                    up.set(dp.withTrait(BlockTraits.IS_UPPER_BLOCK, true), true);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
        if (hand.getBehavior().isShears()) {
            //todo enchantment
            return new ItemStack[]{
                    ItemStack.get(block.getState().resetTrait(BlockTraits.IS_UPPER_BLOCK))
            };
        }

        if (dropSeeds) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.WHEAT_SEEDS)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_SHEARS;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
