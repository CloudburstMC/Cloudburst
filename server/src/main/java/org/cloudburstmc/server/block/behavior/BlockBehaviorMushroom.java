package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import net.daporkchop.lib.random.impl.ThreadLocalPRandom;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.feature.WorldFeature;
import org.cloudburstmc.server.level.feature.tree.GenerationTreeSpecies;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

public abstract class BlockBehaviorMushroom extends FloodableBlockBehavior {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (!canStay(block)) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (canStay(block)) {
            placeBlock(block, item);
            return true;
        }
        return false;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getId() == ItemTypes.DYE && item.getMeta() == DyeColor.WHITE.getDyeData()) {
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }

            if (ThreadLocalRandom.current().nextFloat() < 0.4) {
                this.grow(block);
            }

            block.getLevel().addParticle(new BoneMealParticle(block.getPosition()));
            return true;
        }
        return false;
    }

    public boolean grow(Block block) {
        block.set(BlockStates.AIR, true, false);

        val item = ItemStack.get(block.getState());
        WorldFeature feature = GenerationTreeSpecies.fromItem(item.getId(), item.getMeta()).getDefaultGenerator();

        if (feature.place(block.getLevel(), ThreadLocalPRandom.current(), block.getX(), block.getY(), block.getZ())) {
            return true;
        } else {
            block.set(block.getState(), true, false);
            return false;
        }
    }

    public boolean canStay(Block block) {
        val state = block.down().getState();
        return state.getType() == BlockTypes.MYCELIUM || state.getType() == BlockTypes.PODZOL ||
                (!state.inCategory(BlockCategory.TRANSPARENT) && block.getLevel().getFullLight(block.getPosition()) < 13);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    protected abstract int getType();
}
