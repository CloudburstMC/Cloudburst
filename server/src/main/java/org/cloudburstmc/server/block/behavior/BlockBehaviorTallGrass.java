package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemIds.DYE;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorTallGrass extends FloodableBlockBehavior {

    public BlockBehaviorTallGrass(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean canBeReplaced() {
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
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        // Prevents from placing the same plant block on itself
        if (item.getId() == target.getId() && item.getMeta() == blockState.getMeta()) {
            return false;
        }
        BlockState down = this.down();
        if (down.getId() == GRASS || down.getId() == DIRT || down.getId() == PODZOL) {
            this.getLevel().setBlock(blockState.getPosition(), this, true);
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().isTransparent()) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.getId() == DYE && item.getMeta() == 0x0f) {
            BlockState up = this.up();

            if (up.getId() == AIR) {
                int meta;

                switch (this.getMeta()) {
                    case 0:
                    case 1:
                        meta = BlockBehaviorDoublePlant.TALL_GRASS;
                        break;
                    case 2:
                    case 3:
                        meta = BlockBehaviorDoublePlant.LARGE_FERN;
                        break;
                    default:
                        meta = -1;
                }

                if (meta != -1) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }

                    this.level.addParticle(new BoneMealParticle(this.getPosition()));
                    this.level.setBlock(this.getPosition(), get(DOUBLE_PLANT, meta), true, false);
                    this.level.setBlock(up.getPosition(), get(DOUBLE_PLANT, meta ^ BlockBehaviorDoublePlant.TOP_HALF_BITMASK), true);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public Item[] getDrops(Item hand) {
        boolean dropSeeds = ThreadLocalRandom.current().nextDouble(100) > 87.5;
        if (hand.isShears()) {
            //todo enchantment
            return new Item[]{
                    Item.get(BlockTypes.TALL_GRASS, this.getMeta(), 1)
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
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
