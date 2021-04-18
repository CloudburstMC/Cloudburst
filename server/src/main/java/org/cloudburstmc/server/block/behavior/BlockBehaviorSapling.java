package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import net.daporkchop.lib.random.impl.ThreadLocalPRandom;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.feature.tree.GenerationTreeSpecies;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.api.item.ItemTypes.DYE;

public class BlockBehaviorSapling extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        var type = block.down().getState().getType();
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
        if (item.getType() == DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) { //BoneMeal
            if (player != null && player.getGamemode().isSurvival()) {
                ((CloudPlayer) player).getInventory().decrementHandCount();
            }

            ((CloudLevel) block.getLevel()).addParticle(new BoneMealParticle(block.getPosition()));
            if (ThreadLocalRandom.current().nextFloat() >= 0.45) {
                return true;
            }

            this.grow(block);

            return true;
        }
        return false;
    }

    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == CloudLevel.BLOCK_UPDATE_RANDOM) { //Growth
            if (ThreadLocalRandom.current().nextInt(1, 8) == 1) {
                var state = block.getState();
                if (state.ensureTrait(BlockTraits.HAS_AGE)) {
                    this.grow(block);
                } else {
                    block.set(state.withTrait(BlockTraits.HAS_AGE, true));
                    return CloudLevel.BLOCK_UPDATE_RANDOM;
                }
            } else {
                return CloudLevel.BLOCK_UPDATE_RANDOM;
            }
        }
        return CloudLevel.BLOCK_UPDATE_NORMAL;
    }

    private void grow(Block block) {
        boolean bigTree;

        int x = 0;
        int z = 0;

        var state = block.getState();
        var level = block.getLevel();
        GenerationTreeSpecies species = GenerationTreeSpecies.fromItem(state.getType().getId(), BlockStateMetaMappings.getMetaFromState(state));
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
            level.setBlockState(block.getPosition().add(x, 0, z), BlockStates.AIR, true, false);
            level.setBlockState(block.getPosition().add(x + 1, 0, z), BlockStates.AIR, true, false);
            level.setBlockState(block.getPosition().add(x, 0, z + 1), BlockStates.AIR, true, false);
            level.setBlockState(block.getPosition().add(x + 1, 0, z + 1), BlockStates.AIR, true, false);
        } else {
            feature = species.getDefaultGenerator();
            if (feature == null) {
                return;
            }

            block.set(BlockStates.AIR, true, false);
        }

        if (!feature.place(level, ThreadLocalPRandom.current(), block.getX() + x, block.getY(), block.getZ() + z)) {
            if (bigTree) {
                level.setBlockState(block.getPosition().add(x, 0, z), state, true, false);
                level.setBlockState(block.getPosition().add(x + 1, 0, z), state, true, false);
                level.setBlockState(block.getPosition().add(x, 0, z + 1), state, true, false);
                level.setBlockState(block.getPosition().add(x + 1, 0, z + 1), state, true, false);
            } else {
                level.setBlockState(block.getPosition(), state, true, false);
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
        var blockState = block.getState();

        BlockState state = block.getLevel().getBlock(pos).getState();
        return state.getType() == blockState.getType() && state.ensureTrait(BlockTraits.TREE_SPECIES) == blockState.ensureTrait(BlockTraits.TREE_SPECIES);
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().withTrait(BlockTraits.HAS_AGE, BlockTraits.HAS_AGE.getDefaultValue()));
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
