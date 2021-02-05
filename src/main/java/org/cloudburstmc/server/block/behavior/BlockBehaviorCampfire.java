package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Campfire;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.behavior.ItemEdibleBehavior;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * @author Sleepybear
 */
public class BlockBehaviorCampfire extends BlockBehaviorSolid {

    private static final int CAMPFIRE_LIT_MASK = 0x04; // Bit is 1 when fire is extinguished
    private static final int CAMPFIRE_FACING_MASK = 0x03;


    public boolean isLit(Block block) {
        return !block.getState().ensureTrait(BlockTraits.IS_EXTINGUISHED);
    }

    public void toggleFire(Block block) {
        if (!this.isLit(block) && block.isWaterlogged()) return;

        block.set(block.getState().toggleTrait(BlockTraits.IS_EXTINGUISHED));
        Campfire cf = (Campfire) block.getLevel().getBlockEntity(block.getPosition());
        if (cf != null) block.getLevel().getBlockEntity(block.getPosition()).scheduleUpdate();
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        BlockState state = block.getState();
        if (!state.getBehavior().canBeReplaced(block)) return false;
        if (block.down().getState().getType() == BlockTypes.CAMPFIRE) {
            return false;
        }

        BlockState campfire = BlockRegistry.get().getBlock(BlockTypes.CAMPFIRE)
                .withTrait(BlockTraits.DIRECTION, player.getHorizontalDirection().getOpposite());

        if (placeBlock(block, campfire)) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.CAMPFIRE, block.getChunk(), block.getPosition());
            return true;
        }

        return false;
    }

    @Override
    public int getLightLevel(Block block) {
        return isLit(block) ? 15 : 0;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item) {
        return this.onActivate(block, item, null);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getEnchantment(EnchantmentTypes.SILK_TOUCH) != null) {
            return super.getDrops(block, hand);
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        if (item.getType() == ItemTypes.FLINT_AND_STEEL
                || item.getEnchantment(EnchantmentTypes.FIRE_ASPECT) != null) {
            if (!(this.isLit(block))) {
                this.toggleFire(block);
            }
            return true;
        } else if (item.getBehavior().isShovel()) {
            if (this.isLit(block)) {
                this.toggleFire(block);
            }
            return true;
        } else if (item.getBehavior() instanceof ItemEdibleBehavior) { //TODO: edible items
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

            if (blockEntity instanceof Campfire) {
                Campfire fire = (Campfire) blockEntity;

                if (fire.putItemInFire(item)) {
                    if (player != null && player.isSurvival()) {
                        player.getInventory().decrementHandCount();
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (this.isLit(block) && entity instanceof EntityLiving) {
            entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.FIRE_TICK, 1.0f));
        } else if (!this.isLit(block) && entity.isOnFire()) {
            this.toggleFire(block);
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (this.isLit(block)) {
                if (block.isWaterlogged()) {
                    this.toggleFire(block);
                    return type;
                }

                Block up = block.up();
                if (up.getState().getType() == BlockTypes.WATER || up.getState().getType() == BlockTypes.FLOWING_WATER) {
                    this.toggleFire(block);
                    return type;
                }
            }
            return type;
        }

        return 0;
    }
}
