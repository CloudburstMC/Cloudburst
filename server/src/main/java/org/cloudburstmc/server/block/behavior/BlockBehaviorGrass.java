package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockSpreadEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorGrass extends BlockBehaviorDirt {

    public BlockBehaviorGrass(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0F) {
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }
            this.level.addParticle(new BoneMealParticle(this.getPosition()));

            PRandom random = new FastPRandom();

            for (int i = 0; i < 64; i++) {
                int blockY = this.getY() + random.nextInt(4) - random.nextInt(4);
                if (blockY < 0 || blockY >= 255) {
                    continue;
                }
                int blockX = this.getX() + random.nextInt(8) - random.nextInt(8);
                int blockZ = this.getZ() + random.nextInt(8) - random.nextInt(8);

                BlockState tallGrass = BlockState.get(BlockTypes.TALL_GRASS, 0, blockX, blockY + 1, blockZ, this.level);
                BlockState toReplace = this.level.getBlock(blockX, blockY + 1, blockZ);
                if (toReplace.getId() == BlockTypes.AIR) {
                    tallGrass.place(null, toReplace, null, null, null, null);
                }
            }
            return true;
        } else if (item.isHoe()) {
            item.useOn(this);
            this.getLevel().setBlock(this.getPosition(), BlockState.get(FARMLAND));
            return true;
        } else if (item.isShovel()) {
            item.useOn(this);
            this.getLevel().setBlock(this.getPosition(), BlockState.get(GRASS_PATH));
            return true;
        }

        return false;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            Vector3i pos = this.getPosition();
            int x = ThreadLocalRandom.current().nextInt(pos.getX() - 1, pos.getX() + 1);
            int y = ThreadLocalRandom.current().nextInt(pos.getY() - 2, pos.getY() + 2);
            int z = ThreadLocalRandom.current().nextInt(pos.getZ() - 1, pos.getZ() + 1);
            BlockState blockState = this.getLevel().getBlock(x, y, z);
            if (blockState.getId() == DIRT && blockState.getMeta() == 0) {
                if (blockState.up() instanceof BlockBehaviorAir) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(blockState, this, BlockState.get(GRASS));
                    Server.getInstance().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(blockState.getPosition(), ev.getNewState());
                    }
                }
            } else if (blockState.getId() == GRASS) {
                if (blockState.up() instanceof BlockBehaviorSolid) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(blockState, this, BlockState.get(DIRT));
                    Server.getInstance().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(blockState.getPosition(), ev.getNewState());
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.GRASS_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
