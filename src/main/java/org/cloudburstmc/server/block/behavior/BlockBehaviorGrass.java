package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DirtType;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;

public class BlockBehaviorGrass extends BlockBehaviorDirt {


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        var level = block.getLevel();
        var behavior = item.getBehavior();

        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) {
            if (player != null && player.getGamemode().isSurvival()) {
                player.getInventory().decrementHandCount();
            }
            ((CloudLevel) level).addParticle(new BoneMealParticle(block.getPosition()));

            PRandom random = new FastPRandom();

            for (int i = 0; i < 64; i++) {
                int blockY = block.getY() + random.nextInt(4) - random.nextInt(4);
                if (blockY < 0 || blockY >= 255) {
                    continue;
                }
                int blockX = block.getX() + random.nextInt(8) - random.nextInt(8);
                int blockZ = block.getZ() + random.nextInt(8) - random.nextInt(8);

                BlockState tallGrass = CloudBlockRegistry.get().getBlock(BlockTypes.TALL_GRASS);
                var toReplace = level.getBlock(blockX, blockY + 1, blockZ);
                if (toReplace.getState().getType() == BlockTypes.AIR) {
                    tallGrass.getBehavior().place(null, toReplace, block, Direction.UP, block.getPosition().toFloat(), null);
                }
            }
            return true;
        } else if (behavior.isHoe()) {
            behavior.useOn(item, block.getState());
            block.set(CloudBlockRegistry.get().getBlock(FARMLAND));
            return true;
        } else if (behavior.isShovel()) {
            behavior.useOn(item, block.getState());
            block.set(CloudBlockRegistry.get().getBlock(GRASS_PATH));
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
            var state = b.getState();

            if (state.getType() == DIRT && state.ensureTrait(BlockTraits.DIRT_TYPE) == DirtType.NORMAL) {
                if (b.upState() == BlockStates.AIR) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(b, block, CloudBlockRegistry.get().getBlock(GRASS));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.getLevel().setBlockState(b.getPosition(), ev.getNewState());
                    }
                }
            } else if (state.getType() == GRASS) {
                var up = b.upState();
                if (up.getBehavior().isSolid(up)) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(b, block, CloudBlockRegistry.get().getBlock(DIRT));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.getLevel().setBlockState(b.getPosition(), ev.getNewState());
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
