package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import net.daporkchop.lib.random.impl.ThreadLocalPRandom;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.feature.tree.TreeSpecies;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemIds.DYE;

public class BlockBehaviorSapling extends FloodableBlockBehavior {

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down.getId() == GRASS || down.getId() == DIRT || down.getId() == FARMLAND || down.getId() == PODZOL) {
            this.getLevel().setBlock(block.getPosition(), this, true, true);
            return true;
        }

        return false;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == DYE && item.getMeta() == 0x0F) { //BoneMeal
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }

            this.level.addParticle(new BoneMealParticle(this.getPosition()));
            if (ThreadLocalRandom.current().nextFloat() >= 0.45) {
                return true;
            }

            this.grow();

            return true;
        }
        return false;
    }

    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().isTransparent()) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) { //Growth
            if (ThreadLocalRandom.current().nextInt(1, 8) == 1) {
                if ((this.getMeta() & 0x08) == 0x08) {
                    this.grow();
                } else {
                    this.setMeta(this.getMeta() | 0x08);
                    this.getLevel().setBlock(this.getPosition(), this, true);
                    return Level.BLOCK_UPDATE_RANDOM;
                }
            } else {
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }
        return Level.BLOCK_UPDATE_NORMAL;
    }

    private void grow() {
        boolean bigTree = false;

        int x = 0;
        int z = 0;

        TreeSpecies species = TreeSpecies.fromItem(this.getId(), this.getMeta());
        WorldFeature feature = species.getHugeGenerator();
        BIG_TREE:
        if (bigTree = feature != null) {
            for (int dx = 0; dx >= -1; dx--) {
                for (int dz = 0; dz >= -1; dz--) {
                    if (this.findSaplings(x + dx, z + dz, species.getItemDamage())) {
                        x += dx;
                        z += dz;
                        break BIG_TREE;
                    }
                }
            }
            bigTree = false;
        }

        if (bigTree) {
            this.level.setBlock(this.getPosition().add(x, 0, z), BlockState.get(AIR), true, false);
            this.level.setBlock(this.getPosition().add(x + 1, 0, z), BlockState.get(AIR), true, false);
            this.level.setBlock(this.getPosition().add(x, 0, z + 1), BlockState.get(AIR), true, false);
            this.level.setBlock(this.getPosition().add(x + 1, 0, z + 1), BlockState.get(AIR), true, false);
        } else {
            x = z = 0;
            feature = species.getDefaultGenerator();
            if (feature == null) {
                return;
            }

            this.level.setBlock(this.getPosition(), BlockState.get(AIR), true, false);
        }

        if (!feature.place(this.level, ThreadLocalPRandom.current(), this.getX() + x, this.getY(), this.getZ() + z)) {
            if (bigTree) {
                this.level.setBlock(this.getPosition().add(x, 0, z), this, true, false);
                this.level.setBlock(this.getPosition().add(x + 1, 0, z), this, true, false);
                this.level.setBlock(this.getPosition().add(x, 0, z + 1), this, true, false);
                this.level.setBlock(this.getPosition().add(x + 1, 0, z + 1), this, true, false);
            } else {
                this.level.setBlock(this.getPosition(), this, true, false);
            }
        }
    }

    private boolean findSaplings(int x, int z, int type) {
        return this.isSameType(this.getPosition().add(x, 0, z), type) &&
                this.isSameType(this.getPosition().add(x + 1, 0, z), type) &&
                this.isSameType(this.getPosition().add(x, 0, z + 1), type) &&
                this.isSameType(this.getPosition().add(x + 1, 0, z + 1), type);
    }

    public boolean isSameType(Vector3i pos, int type) {
        BlockState blockState = this.level.getBlock(pos);
        return blockState.getId() == this.getId() && (blockState.getMeta() & 0x07) == (type & 0x07);
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(SAPLING, this.getMeta() & 0x7);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
