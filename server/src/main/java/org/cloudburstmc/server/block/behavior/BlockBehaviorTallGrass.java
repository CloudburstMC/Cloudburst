package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.TallGrassType;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.api.item.ItemTypes.DYE;

public class BlockBehaviorTallGrass extends FloodableBlockBehavior {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        // Prevents from placing the same plant block on itself
        var itemBlock = item.getBehavior().getBlock(item);
        if (itemBlock.getType() == target.getState().getType() && itemBlock.ensureTrait(BlockTraits.TALL_GRASS_TYPE) == block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE)) {
            return false;
        }
        var down = block.down().getState().getType();
        if (down == GRASS || down == DIRT || down == PODZOL) {
            placeBlock(block, itemBlock);
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) {
            var up = block.up();

            if (up.getState().getType() == AIR) {
                var type = block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE);
                if (type == TallGrassType.DEFAULT) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        ((CloudPlayer) player).getInventory().decrementHandCount();
                    }

                    ((CloudLevel) block.getLevel()).addParticle(new BoneMealParticle(block.getPosition()));

                    var dp = CloudBlockRegistry.get().getBlock(DOUBLE_PLANT).withTrait(BlockTraits.TALL_GRASS_TYPE, type);
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
                    CloudItemRegistry.get().getItem(block.getState().withTrait(BlockTraits.IS_UPPER_BLOCK, BlockTraits.IS_UPPER_BLOCK.getDefaultValue()))
            };
        }

        if (dropSeeds) {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(ItemTypes.WHEAT_SEEDS)
            };
        } else {
            return new ItemStack[0];
        }
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
