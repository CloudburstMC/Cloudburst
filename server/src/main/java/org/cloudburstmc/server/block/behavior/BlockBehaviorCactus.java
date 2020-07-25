package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorCactus extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 0.4f;
    }

    @Override
    public float getResistance() {
        return 2;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public float getMinX() {
        return this.getX() + 0.0625f;
    }

    @Override
    public float getMinY() {
        return this.getY();
    }

    @Override
    public float getMinZ() {
        return this.getZ() + 0.0625f;
    }

    @Override
    public float getMaxX() {
        return this.getX() + 0.9375f;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.9375f;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + 0.9375f;
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return new SimpleAxisAlignedBB(this.getX(), this.getY(), this.getZ(), this.getX() + 1, this.getY() + 1, this.getZ() + 1);
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.CONTACT, 1));
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            BlockState down = down();
            if (down.getId() != SAND && down.getId() != CACTUS) {
                this.getLevel().useBreakOn(this.getPosition());
            } else {
                for (int side = 2; side <= 5; ++side) {
                    BlockState blockState = getSide(Direction.fromIndex(side));
                    if (!blockState.canBeFlooded()) {
                        this.getLevel().useBreakOn(this.getPosition());
                    }
                }
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (down().getId() != CACTUS) {
                if (this.getMeta() == 0x0F) {
                    for (int y = 1; y < 3; ++y) {
                        BlockState b = this.getLevel().getBlock(this.getX(), this.getY() + y, this.getZ());
                        if (b.getId() == AIR) {
                            BlockGrowEvent event = new BlockGrowEvent(b, BlockState.get(CACTUS));
                            Server.getInstance().getPluginManager().callEvent(event);
                            if (!event.isCancelled()) {
                                this.getLevel().setBlock(b.getPosition(), event.getNewState(), true);
                            }
                        }
                    }
                    this.setMeta(0);
                    this.getLevel().setBlock(this.getPosition(), this);
                } else {
                    this.setMeta(this.getMeta() + 1);
                    this.getLevel().setBlock(this.getPosition(), this);
                }
            }
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (!blockState.isWaterlogged() && (down.getId() == SAND || down.getId() == CACTUS)) {
            BlockState blockState0 = north();
            BlockState blockState1 = south();
            BlockState blockState2 = west();
            BlockState blockState3 = east();
            if (blockState0.canBeFlooded() && blockState1.canBeFlooded() && blockState2.canBeFlooded() && blockState3.canBeFlooded()) {
                this.getLevel().setBlock(this.getPosition(), this, true);

                return true;
            }
        }
        return false;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(CACTUS, 0, 1)
        };
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
