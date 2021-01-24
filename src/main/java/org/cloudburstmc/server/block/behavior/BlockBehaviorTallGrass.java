package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.TallGrassType;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.*;
import static org.cloudburstmc.server.item.behavior.ItemIds.DYE;

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
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        // Prevents from placing the same plant block on itself
        val itemBlock = item.getBlock();
        if (itemBlock.getType() == target.getState().getType() && itemBlock.ensureTrait(BlockTraits.TALL_GRASS_TYPE) == block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE)) {
            return false;
        }
        val down = block.down().getState().getType();
        if (down == GRASS || down == DIRT || down == PODZOL) {
            placeBlock(block, item.getBlock());
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == DYE && item.getMeta() == 0x0f) {
            val up = block.up();

            if (up.getState().getType() == AIR) {
                val type = block.getState().ensureTrait(BlockTraits.TALL_GRASS_TYPE);
                if (type == TallGrassType.DEFAULT) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }

                    block.getWorld().addParticle(new BoneMealParticle(block.getPosition()));

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
    public Item[] getDrops(Block block, Item hand) {
        boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
        if (hand.isShears()) {
            //todo enchantment
            return new Item[]{
                    Item.get(block.getState().resetTrait(BlockTraits.IS_UPPER_BLOCK))
            };
        }

        if (dropSeeds) {
            return new Item[]{
                    Item.get(ItemIds.WHEAT_SEEDS)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHEARS;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
