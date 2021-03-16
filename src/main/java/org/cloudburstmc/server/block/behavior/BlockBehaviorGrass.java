package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.data.DirtType;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;

public class BlockBehaviorGrass extends BlockBehaviorDirt {


    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        val level = block.getLevel();
        val behavior = item.getBehavior();

        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) {
            if (player != null && player.getGamemode().isSurvival()) {
                player.getInventory().decrementHandCount();
            }
            level.addParticle(new BoneMealParticle(block.getPosition()));

            PRandom random = new FastPRandom();

            for (int i = 0; i < 64; i++) {
                int blockY = block.getY() + random.nextInt(4) - random.nextInt(4);
                if (blockY < 0 || blockY >= 255) {
                    continue;
                }
                int blockX = block.getX() + random.nextInt(8) - random.nextInt(8);
                int blockZ = block.getZ() + random.nextInt(8) - random.nextInt(8);

                BlockState tallGrass = BlockState.get(BlockTypes.TALL_GRASS);
                val toReplace = level.getBlock(blockX, blockY + 1, blockZ);
                if (toReplace.getState().getType() == BlockTypes.AIR) {
                    tallGrass.getBehavior().place(null, toReplace, block, Direction.UP, block.getPosition().toFloat(), null);
                }
            }
            return true;
        } else if (behavior.isHoe()) {
            behavior.useOn(item, block);
            block.set(BlockState.get(FARMLAND));
            return true;
        } else if (behavior.isShovel()) {
            behavior.useOn(item, block);
            block.set(BlockState.get(GRASS_PATH));
            return true;
        }

        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            Vector3i pos = block.getPosition();
            int x = ThreadLocalRandom.current().nextInt(pos.getX() - 1, pos.getX() + 1);
            int y = ThreadLocalRandom.current().nextInt(pos.getY() - 2, pos.getY() + 2);
            int z = ThreadLocalRandom.current().nextInt(pos.getZ() - 1, pos.getZ() + 1);
            Block b = block.getLevel().getBlock(x, y, z);
            val state = b.getState();

            if (state.getType() == DIRT && state.ensureTrait(BlockTraits.DIRT_TYPE) == DirtType.NORMAL) {
                if (b.upState() == BlockStates.AIR) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(b, block, BlockState.get(GRASS));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.getLevel().setBlock(b.getPosition(), ev.getNewState());
                    }
                }
            } else if (state.getType() == GRASS) {
                val up = b.upState();
                if (up.getBehavior().isSolid(up)) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(b, block, BlockState.get(DIRT));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.getLevel().setBlock(b.getPosition(), ev.getNewState());
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.GRASS_BLOCK_COLOR;
    }


}
