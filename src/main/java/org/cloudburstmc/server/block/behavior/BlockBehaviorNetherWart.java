package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;

/**
 * Created by Leonidius20 on 22.03.17.
 */
public class BlockBehaviorNetherWart extends FloodableBlockBehavior {

    public BlockBehaviorNetherWart(Identifier id) {
        super(id);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down.getId() == BlockTypes.SOUL_SAND) {
            this.getLevel().setBlock(blockState.getPosition(), this, true, true);
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() != BlockTypes.SOUL_SAND) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (new Random().nextInt(10) == 1) {
                if (this.getMeta() < 0x03) {
                    BlockBehaviorNetherWart block = (BlockBehaviorNetherWart) this.clone();
                    block.setMeta(block.getMeta() + 1);
                    BlockGrowEvent ev = new BlockGrowEvent(this, block);
                    Server.getInstance().getPluginManager().callEvent(ev);

                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(this.getPosition(), ev.getNewState(), true, true);
                    } else {
                        return Level.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }

        return 0;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.RED_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (this.getMeta() == 0x03) {
            return new Item[]{
                    Item.get(ItemIds.NETHER_WART, 0, 2 + (int) (Math.random() * ((4 - 2) + 1)))
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.NETHER_WART)
            };
        }
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.NETHER_WART);
    }
}


