package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.*;
import static org.cloudburstmc.server.item.ItemIds.DYE;

public class BlockBehaviorFlower extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val down = block.down().getState().getType();
        if (down == GRASS || down == DIRT || down == FARMLAND || down == PODZOL) {
            placeBlock(block, getUncommonFlower());
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
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
        if (item.getId() == DYE && item.getMeta() == 0x0f) { //Bone meal
            val level = block.getLevel();
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }

            level.addParticle(new BoneMealParticle(block.getPosition()));

            for (int i = 0; i < 8; i++) {
                Vector3i vec = block.getPosition().add(
                        ThreadLocalRandom.current().nextInt(-3, 4),
                        ThreadLocalRandom.current().nextInt(-1, 2),
                        ThreadLocalRandom.current().nextInt(-3, 4));

                if (level.getBlock(vec).getState().getType() == AIR && level.getBlock(vec.down()).getState().getType() == GRASS && vec.getY() >= 0 && vec.getY() < 256) {
                    if (ThreadLocalRandom.current().nextInt(10) == 0) {
                        level.setBlock(vec, this.getUncommonFlower(), true);
                    } else {
                        level.setBlock(vec, BlockState.get(block.getState().getType()), true);
                    }
                }
            }

            return true;
        }

        return false;
    }

    protected BlockState getUncommonFlower() {
        return BlockState.get(YELLOW_FLOWER);
    }
}
