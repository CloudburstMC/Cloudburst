package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import net.daporkchop.lib.random.impl.ThreadLocalPRandom;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.feature.tree.GenerationTreeSpecies;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemTypes.DYE;

public class BlockBehaviorSapling extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val type = block.down().getState().getType();
        if (type == GRASS || type == DIRT || type == FARMLAND || type == PODZOL) {
            placeBlock(block, item);
            return true;
        }

        return false;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getId() == DYE && item.getMeta() == 0x0F) { //BoneMeal
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }

            block.getLevel().addParticle(new BoneMealParticle(block.getPosition()));
            if (ThreadLocalRandom.current().nextFloat() >= 0.45) {
                return true;
            }

            this.grow(block);

            return true;
        }
        return false;
    }

    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) { //Growth
            if (ThreadLocalRandom.current().nextInt(1, 8) == 1) {
                val state = block.getState();
                if (state.ensureTrait(BlockTraits.HAS_AGE)) {
                    this.grow(block);
                } else {
                    block.set(state.withTrait(BlockTraits.HAS_AGE, true));
                    return Level.BLOCK_UPDATE_RANDOM;
                }
            } else {
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }
        return Level.BLOCK_UPDATE_NORMAL;
    }

    private void grow(Block block) {
        boolean bigTree;

        int x = 0;
        int z = 0;

        val state = block.getState();
        val level = block.getLevel();
        GenerationTreeSpecies species = GenerationTreeSpecies.fromItem(state.getType(), BlockStateMetaMappings.getMetaFromState(state));
        WorldFeature feature = species.getHugeGenerator();
        BIG_TREE:
        if (bigTree = feature != null) {
            for (int dx = 0; dx >= -1; dx--) {
                for (int dz = 0; dz >= -1; dz--) {
                    if (this.findSaplings(block, x + dx, z + dz)) {
                        x += dx;
                        z += dz;
                        break BIG_TREE;
                    }
                }
            }
            bigTree = false;
        }

        if (bigTree) {
            level.setBlock(block.getPosition().add(x, 0, z), BlockStates.AIR, true, false);
            level.setBlock(block.getPosition().add(x + 1, 0, z), BlockStates.AIR, true, false);
            level.setBlock(block.getPosition().add(x, 0, z + 1), BlockStates.AIR, true, false);
            level.setBlock(block.getPosition().add(x + 1, 0, z + 1), BlockStates.AIR, true, false);
        } else {
            feature = species.getDefaultGenerator();
            if (feature == null) {
                return;
            }

            block.set(BlockStates.AIR, true, false);
        }

        if (!feature.place(level, ThreadLocalPRandom.current(), block.getX() + x, block.getY(), block.getZ() + z)) {
            if (bigTree) {
                level.setBlock(block.getPosition().add(x, 0, z), state, true, false);
                level.setBlock(block.getPosition().add(x + 1, 0, z), state, true, false);
                level.setBlock(block.getPosition().add(x, 0, z + 1), state, true, false);
                level.setBlock(block.getPosition().add(x + 1, 0, z + 1), state, true, false);
            } else {
                level.setBlock(block.getPosition(), state, true, false);
            }
        }
    }

    private boolean findSaplings(Block block, int x, int z) {
        return this.isSameType(block, block.getPosition().add(x, 0, z)) &&
                this.isSameType(block, block.getPosition().add(x + 1, 0, z)) &&
                this.isSameType(block, block.getPosition().add(x, 0, z + 1)) &&
                this.isSameType(block, block.getPosition().add(x + 1, 0, z + 1));
    }

    public boolean isSameType(Block block, Vector3i pos) {
        val blockState = block.getState();

        BlockState state = block.getLevel().getBlock(pos).getState();
        return state.getType() == blockState.getType() && state.ensureTrait(BlockTraits.TREE_SPECIES) == blockState.ensureTrait(BlockTraits.TREE_SPECIES);
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().resetTrait(BlockTraits.HAS_AGE));
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
