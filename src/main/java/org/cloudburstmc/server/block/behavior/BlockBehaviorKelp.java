package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTraits.FLUID_LEVEL;
import static org.cloudburstmc.server.block.BlockTraits.KELP_AGE;
import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.math.Direction.DOWN;
import static org.cloudburstmc.server.math.MathHelper.clamp;

public class BlockBehaviorKelp extends FloodableBlockBehavior {

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Integer waterDamage = block.getExtra().getState().getTrait(FLUID_LEVEL);
        if (waterDamage == null || waterDamage != 0 && waterDamage != 8) {
            return false;
        }

        Block down = block.getSide(DOWN);
        BlockState downState = down.getState();
        if ((downState.getType() != KELP && !down.getBehavior().isSolid())
                || downState.getType() == MAGMA || downState.getType() == ICE || downState.getType() == SOUL_SAND) {
            return false;
        }

        if (waterDamage == 8) {
            block.getLevel().setBlock(block.getPosition(), 1, BlockState.get(FLOWING_WATER), true, false);
        }

        if (downState.getType() == KELP && downState.ensureTrait(KELP_AGE) != 24) {
            block.set(downState.withTrait(KELP_AGE, 24), true, true);
        }
        BlockState newState = block.getState().withTrait(KELP_AGE, ThreadLocalRandom.current().nextInt(25));
        block.set(newState, true, true);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            Integer waterDamage = block.getState().getTrait(FLUID_LEVEL);
            if (waterDamage == null || waterDamage != 0 && waterDamage != 8) {
                block.getLevel().useBreakOn(block.getPosition());
                return type;
            }

            Block down = block.getSide(DOWN);
            BlockState downState = down.getState();
            if ((downState.getType() != KELP && !down.getBehavior().isSolid())
                    || downState.getType() == MAGMA || downState.getType() == ICE || downState.getType() == SOUL_SAND) {
                block.getLevel().useBreakOn(block.getPosition());
                return type;
            }

            if (waterDamage == 8) {
                block.getLevel().setBlock(block.getPosition(), 1, BlockState.get(FLOWING_WATER), true, false);
            }
            return type;
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(100) <= 14) {
                grow();
            }
            return type;
        }
        return super.onUpdate(block, type);
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
    public boolean onBreak(Block block, Item item) {
        BlockState down = down();
        if (down.getId() == KELP) {
            this.getLevel().setBlock(down.getPosition(), BlockState.get(KELP, ThreadLocalRandom.current().nextInt(25)), true, true);
        }
        super.onBreak(block, item);
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
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
    public Item toItem(Block block) {
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
