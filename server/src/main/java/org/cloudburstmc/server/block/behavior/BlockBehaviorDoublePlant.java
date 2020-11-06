package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DoublePlantType;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorDoublePlant extends FloodableBlockBehavior {

    @Override
    public boolean canBeReplaced(Block block) {
        DoublePlantType type = block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE);
        return type == DoublePlantType.GRASS || type == DoublePlantType.FERN;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
                // Top
                if (!(block.down().getState().getType() == DOUBLE_PLANT)) {
                    removeBlock(block, true);
                    return Level.BLOCK_UPDATE_NORMAL;
                }
            } else {
                // Bottom
                if (block.down().getState().inCategory(BlockCategory.TRANSPARENT) || block.up().getState().getType() != DOUBLE_PLANT) {
                    block.getLevel().useBreakOn(block.getPosition());
                    return Level.BLOCK_UPDATE_NORMAL;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = block.down().getState();
        Block up = block.up();

        if (up.getState().getType() == AIR && (down.getType() == GRASS || down.getType() == DIRT)) {
            placeBlock(block, item, false); // If we update the bottom half, it will drop the item because there isn't a flower block above
            placeBlock(up, item.getBehavior().getBlock(item).withTrait(BlockTraits.IS_UPPER_BLOCK, true));
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        Block down = block.down();

        if (block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) { // Top half
            block.getLevel().useBreakOn(down.getPosition());
        } else {
            removeBlock(block, true);
        }

        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (!block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
            boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
            DoublePlantType type = block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE);
            switch (type) {
                case GRASS:
                case FERN:
                    if (hand.getBehavior().isShears()) {
                        //todo enchantment
                        return new ItemStack[]{
                                ItemStack.get(block.getState(), 2)
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

            return new ItemStack[]{toItem(block)};
        }

        return new ItemStack[0];
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) { //Bone meal
            switch (block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE)) {
                case SUNFLOWER:
                case SYRINGA:
                case ROSE:
                case PAEONIA:
                    if (player != null && player.getGamemode().isSurvival()) {
                        player.getInventory().decrementHandCount();
                    }
                    block.getLevel().addParticle(new BoneMealParticle(block.getPosition()));
                    block.getLevel().dropItem(block.getPosition(), CloudItemRegistry.get().getItem(block.getState()));
            }

            return true;
        }

        return false;
    }
}