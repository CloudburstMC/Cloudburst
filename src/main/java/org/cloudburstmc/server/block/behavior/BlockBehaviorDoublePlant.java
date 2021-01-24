package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DoublePlantType;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.*;

public class BlockBehaviorDoublePlant extends FloodableBlockBehavior {

    @Override
    public boolean canBeReplaced(Block block) {
        if (block == null) {
            return true;
        }

        DoublePlantType type = block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE);
        return type == DoublePlantType.GRASS || type == DoublePlantType.FERN;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
                // Top
                if (!(block.down().getState().getType() == DOUBLE_PLANT)) {
                    removeBlock(block, true);
                    return World.BLOCK_UPDATE_NORMAL;
                }
            } else {
                // Bottom
                if (block.down().getState().inCategory(BlockCategory.TRANSPARENT) || block.up().getState().getType() != DOUBLE_PLANT) {
                    block.getWorld().useBreakOn(block.getPosition());
                    return World.BLOCK_UPDATE_NORMAL;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = block.down().getState();
        Block up = block.up();

        if (up.getState().getType() == AIR && (down.getType() == GRASS || down.getType() == DIRT)) {
            placeBlock(block, item, false); // If we update the bottom half, it will drop the item because there isn't a flower block above
            placeBlock(up, item.getBlock().withTrait(BlockTraits.IS_UPPER_BLOCK, true));
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        Block down = block.down();

        if (block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) { // Top half
            block.getWorld().useBreakOn(down.getPosition());
        } else {
            removeBlock(block, true);
        }

        return true;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (!block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
            boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
            DoublePlantType type = block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE);
            switch (type) {
                case GRASS:
                case FERN:
                    if (hand.isShears()) {
                        //todo enchantment
                        return new Item[]{
                                Item.get(BlockIds.TALL_GRASS, type == DoublePlantType.GRASS ? 1 : 2, 2)
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

            return new Item[]{toItem(block)};
        }

        return new Item[0];
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
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0f) { //Bone meal
            switch (block.getState().ensureTrait(BlockTraits.DOUBLE_PLANT_TYPE)) {
                case SUNFLOWER:
                case SYRINGA:
                case ROSE:
                case PAEONIA:
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }
                    block.getWorld().addParticle(new BoneMealParticle(block.getPosition()));
                    block.getWorld().dropItem(block.getPosition(), ItemRegistry.get().getItem(block.getState()));
            }

            return true;
        }

        return false;
    }
}