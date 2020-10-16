package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTraits.FLUID_LEVEL;
import static org.cloudburstmc.server.block.BlockTraits.KELP_AGE;
import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.math.Direction.DOWN;

public class BlockBehaviorKelp extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val state = block.getState();

        if (state.getType() != WATER && state.getType() != FLOWING_WATER) {
            return false;
        }

        int waterLevel = state.ensureTrait(FLUID_LEVEL);
        if (waterLevel != 0) {
            return false;
        }

        Block down = block.getSide(DOWN);
        BlockState downState = down.getState();
        if ((downState.getType() != KELP && !downState.getBehavior().isSolid(downState))
                || downState.getType() == MAGMA || downState.getType() == ICE || downState.getType() == SOUL_SAND) {
            return false;
        }

//        if (waterDamage == 8) { //TODO: check
//            block.getLevel().setBlock(block.getPosition(), 1, BlockState.get(FLOWING_WATER), true, false);
//        }

        if (downState.getType() == KELP && downState.ensureTrait(KELP_AGE) != 24) {
            block.set(downState.withTrait(KELP_AGE, 24), true, true);
        }
        BlockState newState = block.getState().withTrait(KELP_AGE, ThreadLocalRandom.current().nextInt(25));
        placeBlock(block, newState);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            val liquid = block.getExtra();
            Integer waterDamage = liquid.getTrait(FLUID_LEVEL);

            if (waterDamage == null || waterDamage != 0 || liquid.ensureTrait(BlockTraits.IS_FLOWING)) {
                block.getLevel().useBreakOn(block.getPosition());
                return type;
            }

            Block down = block.getSide(DOWN);
            BlockState downState = down.getState();
            if ((downState.getType() != KELP && !downState.inCategory(BlockCategory.SOLID))
                    || downState.getType() == MAGMA || downState.getType() == ICE || downState.getType() == SOUL_SAND) {
                block.getLevel().useBreakOn(block.getPosition());
                return type;
            }

//            if (waterDamage == 8) { //TODO: check
//                block.getLevel().setBlock(block.getPosition(), 1, BlockState.get(FLOWING_WATER), true, false);
//            }
            return type;
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(100) <= 14) {
                grow(block);
            }
            return type;
        }
        return super.onUpdate(block, type);
    }

    public boolean grow(Block block) {
        int age = block.getState().ensureTrait(KELP_AGE);
        if (age < 25) {
            val up = block.up();
            val upState = up.getState();
            if (upState.getType() == WATER || upState.getType() == FLOWING_WATER) {
                int fluidLevel = upState.ensureTrait(FLUID_LEVEL);

                if (fluidLevel != 0 || upState.ensureTrait(BlockTraits.IS_FLOWING)) {
                    return false;
                }

                BlockState grown = block.getState().incrementTrait(KELP_AGE);
                BlockGrowEvent ev = new BlockGrowEvent(block, grown);
                Server.getInstance().getEventManager().fire(ev);
                if (!ev.isCancelled()) {
                    block.set(block.getState().withTrait(KELP_AGE, 25), true);

                    placeBlock(up, ev.getNewState());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        Block down = block.down();

        if (down.getState().getType() == KELP) {
            down.set(block.getState().withTrait(KELP_AGE, ThreadLocalRandom.current().nextInt(25)), true);
        }
        super.onBreak(block, item);
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) { //Bone Meal
            val level = block.getLevel();
            int x = block.getX();
            int z = block.getZ();
            for (int y = block.getY() + 1; y < 255; y++) {
                val above = level.getBlockAt(x, y, z);
                val blockAbove = above.getType();
                if (blockAbove == KELP) {
                    continue;
                }

                if (blockAbove == WATER || blockAbove == FLOWING_WATER) {
                    int waterData = above.ensureTrait(FLUID_LEVEL);
                    if (waterData == 0 && !above.ensureTrait(BlockTraits.IS_FLOWING)) {
                        val highestKelp = level.getBlock(x, y - 1, z);
                        if (grow(highestKelp)) {
                            level.addParticle(new BoneMealParticle(block.getPosition()));

                            if (player != null && !player.isCreative()) {
                                player.getInventory().decrementHandCount();
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
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.KELP);
    }


    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }
}
