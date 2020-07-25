package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorDoublePlant extends FloodableBlockBehavior {
    public static final int SUNFLOWER = 0;
    public static final int LILAC = 1;
    public static final int TALL_GRASS = 2;
    public static final int LARGE_FERN = 3;
    public static final int ROSE_BUSH = 4;
    public static final int PEONY = 5;
    public static final int TOP_HALF_BITMASK = 0x8;

    public BlockBehaviorDoublePlant(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeReplaced() {
        return this.getMeta() == TALL_GRASS || this.getMeta() == LARGE_FERN;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if ((this.getMeta() & TOP_HALF_BITMASK) == TOP_HALF_BITMASK) {
                // Top
                if (!(this.down().getId() == DOUBLE_PLANT)) {
                    this.getLevel().setBlock(this.getPosition(), BlockState.get(AIR), false, true);
                    return Level.BLOCK_UPDATE_NORMAL;
                }
            } else {
                // Bottom
                if (this.down().isTransparent() || !(this.up().getId() == DOUBLE_PLANT)) {
                    this.getLevel().useBreakOn(this.getPosition());
                    return Level.BLOCK_UPDATE_NORMAL;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState down = down();
        BlockState up = up();

        if (up.getId() == AIR && (down.getId() == GRASS || down.getId() == DIRT)) {
            this.getLevel().setBlock(block.getPosition(), this, true, false); // If we update the bottom half, it will drop the item because there isn't a flower block above
            this.getLevel().setBlock(up.getPosition(), BlockState.get(DOUBLE_PLANT, getMeta() ^ TOP_HALF_BITMASK), true, true);
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        Block down = block.down();

        if ((this.getMeta() & TOP_HALF_BITMASK) == TOP_HALF_BITMASK) { // Top half
            block.getLevel().useBreakOn(down.getPosition());
        } else {
            block.set(BlockState.AIR, true, true);
        }

        return true;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if ((this.getMeta() & TOP_HALF_BITMASK) != TOP_HALF_BITMASK) {
            boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
            switch (this.getMeta() & 0x07) {
                case TALL_GRASS:
                case LARGE_FERN:
                    if (hand.isShears()) {
                        //todo enchantment
                        return new Item[]{
                                Item.get(BlockTypes.TALL_GRASS, (this.getMeta() & 0x07) == TALL_GRASS ? 1 : 2, 2)
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

            return new Item[]{toItem(blockState)};
        }

        return new Item[0];
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0f) { //Bone meal
            switch (this.getMeta() & 0x07) {
                case SUNFLOWER:
                case LILAC:
                case ROSE_BUSH:
                case PEONY:
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }
                    this.level.addParticle(new BoneMealParticle(this.getPosition()));
                    this.level.dropItem(this.getPosition(), this.toItem(blockState));
            }

            return true;
        }

        return false;
    }
}