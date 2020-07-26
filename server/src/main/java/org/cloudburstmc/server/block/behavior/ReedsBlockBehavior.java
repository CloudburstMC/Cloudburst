package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class ReedsBlockBehavior extends FloodableBlockBehavior {

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.REEDS);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0F) { //Bonemeal
            int count = 1;

            for (int i = 1; i <= 2; i++) {
                Identifier id = this.level.getBlockId(this.getX(), this.getY() - i, this.getZ());

                if (id == BlockTypes.REEDS) {
                    count++;
                }
            }

            if (count < 3) {
                boolean success = false;
                int toGrow = 3 - count;

                for (int i = 1; i <= toGrow; i++) {
                    BlockState blockState = this.up(i);
                    if (blockState.getId() == BlockTypes.AIR) {
                        BlockGrowEvent ev = new BlockGrowEvent(blockState, BlockState.get(BlockTypes.REEDS));
                        Server.getInstance().getPluginManager().callEvent(ev);

                        if (!ev.isCancelled()) {
                            this.getLevel().setBlock(blockState.getPosition(), ev.getNewState(), true);
                            success = true;
                        }
                    } else if (blockState.getId() != BlockTypes.REEDS) {
                        break;
                    }
                }

                if (success) {
                    if (player != null && player.getGamemode().isSurvival()) {
                        item.decrementCount();
                    }

                    this.level.addParticle(new BoneMealParticle(this.getPosition()));
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            BlockState down = this.down();
            if (down.isTransparent() && down.getId() != BlockTypes.REEDS) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (this.down().getId() != BlockTypes.REEDS) {
                if (this.getMeta() == 0x0F) {
                    for (int y = 1; y < 3; ++y) {
                        BlockState b = this.getLevel().getBlock(this.getX(), this.getY() + y, this.getZ());
                        if (b.getId() == BlockTypes.AIR) {
                            this.getLevel().setBlock(b.getPosition(), BlockState.get(BlockTypes.REEDS), false);
                            break;
                        }
                    }
                    this.setMeta(0);
                    this.getLevel().setBlock(this.getPosition(), this, false);
                } else {
                    this.setMeta(this.getMeta() + 1);
                    this.getLevel().setBlock(this.getPosition(), this, false);
                }
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (blockState.getId() != BlockTypes.AIR) {
            return false;
        }
        BlockState down = this.down();
        if (down.getId() == BlockTypes.REEDS) {
            this.getLevel().setBlock(blockState.getPosition(), BlockState.get(BlockTypes.REEDS), true);
            return true;
        } else if (down.getId() == BlockTypes.GRASS || down.getId() == BlockTypes.DIRT || down.getId() == BlockTypes.SAND) {
            BlockState blockState0 = down.north();
            BlockState blockState1 = down.south();
            BlockState blockState2 = down.west();
            BlockState blockState3 = down.east();
            if ((blockState0 instanceof BlockBehaviorWater) || (blockState1 instanceof BlockBehaviorWater) || (blockState2 instanceof BlockBehaviorWater) || (blockState3 instanceof BlockBehaviorWater)) {
                this.getLevel().setBlock(blockState.getPosition(), BlockState.get(BlockTypes.REEDS), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
