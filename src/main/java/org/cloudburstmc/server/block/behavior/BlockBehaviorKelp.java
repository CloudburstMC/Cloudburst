package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.math.MathHelper.clamp;

public class BlockBehaviorKelp extends FloodableBlockBehavior {

    public BlockBehaviorKelp(Identifier id) {
        super(id);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        int waterDamage = getWaterloggingWaterDamage();
        if (waterDamage != 0 && waterDamage != 8) {
            return false;
        }

        BlockState down = down();
        if ((down.getId() != KELP && !down.isSolid())
                || down.getId() == MAGMA
                || down.getId() == ICE
                || down.getId() == SOUL_SAND
        ) {
            return false;
        }

        if (waterDamage == 8) {
            this.level.setBlock(this.getPosition(), 1, BlockState.get(FLOWING_WATER, 0), true, false);
        }

        if (down.getId() == KELP && down.getMeta() != 24) {
            down.setMeta(24);
            this.level.setBlock(down.getPosition(), down, true, true);
        }
        setMeta(ThreadLocalRandom.current().nextInt(25));
        this.level.setBlock(this.getPosition(), this, true, true);
        return true;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            int waterDamage = getWaterloggingWaterDamage();
            if (waterDamage != 0 && waterDamage != 8) {
                this.getLevel().useBreakOn(this.getPosition());
                return type;
            }

            BlockState down = down();
            if ((!down.isSolid() && down.getId() != KELP)
                    || down.getId() == MAGMA
                    || down.getId() == ICE
                    || down.getId() == SOUL_SAND) {
                this.getLevel().useBreakOn(this.getPosition());
                return type;
            }

            if (waterDamage == 8) {
                this.getLevel().setBlock(this.getPosition(), 1, BlockState.get(FLOWING_WATER, 0), true, false);
            }
            return type;
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(100) <= 14) {
                grow();
            }
            return type;
        }
        return super.onUpdate(type);
    }

    public boolean grow() {
        int age = clamp(getMeta(), 0, 25);
        if (age < 25) {
            BlockState up = up();
            if ((up.getId() == WATER || up.getId() == FLOWING_WATER) && (up.getMeta() == 0 || up.getMeta() == 8)) {
                BlockState grown = BlockState.get(id, age + 1);
                BlockGrowEvent ev = new BlockGrowEvent(this, grown);
                Server.getInstance().getPluginManager().callEvent(ev);
                if (!ev.isCancelled()) {
                    this.setMeta(25);
                    this.getLevel().setBlock(this.getPosition(), this, true, true);
                    if (ev.getNewState().canWaterlogSource()) {
                        this.getLevel().setBlock(up.getPosition(), 1, BlockState.get(FLOWING_WATER, 0), true, false);
                    }
                    this.getLevel().setBlock(up.getPosition(), ev.getNewState(), true, true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        BlockState down = down();
        if (down.getId() == KELP) {
            this.getLevel().setBlock(down.getPosition(), BlockState.get(KELP, ThreadLocalRandom.current().nextInt(25)), true, true);
        }
        super.onBreak(item);
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0f) { //Bone Meal
            int x = getX();
            int z = getZ();
            for (int y = getY() + 1; y < 255; y++) {
                Identifier blockAbove = getLevel().getBlockId(x, y, z);
                if (blockAbove == KELP) {
                    continue;
                }

                if (blockAbove == WATER || blockAbove == FLOWING_WATER) {
                    int waterData = getLevel().getBlockDataAt(x, y, z);
                    if (waterData == 0 || waterData == 8) {
                        BlockBehaviorKelp highestKelp = (BlockBehaviorKelp) getLevel().getBlock(x, y - 1, z);
                        if (highestKelp.grow()) {
                            this.level.addParticle(new BoneMealParticle(this.getPosition()));

                            if (player != null && !player.isCreative()) {
                                item.decrementCount(1);
                            }
                            return false;
                        }
                    }
                }

                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.KELP);
    }


    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }
}
