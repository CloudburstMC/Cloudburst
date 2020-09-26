package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Campfire;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemEdibleBehavior;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * @author Sleepybear
 */
public class BlockBehaviorCampfire extends BlockBehaviorSolid {

    private static final int CAMPFIRE_LIT_MASK = 0x04; // Bit is 1 when fire is extinguished
    private static final int CAMPFIRE_FACING_MASK = 0x03;

    @Override
    public float getHardness() {
        return 2.0f;
    }

    @Override
    public float getResistance() {
        return 10.0f;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

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
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
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
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canBeFlooded() {
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item) {
        return this.onActivate(block, item, null);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getEnchantment(CloudEnchantmentInstance.ID_SILK_TOUCH) != null) {
            return super.getDrops(block, hand);
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getId() == ItemTypes.FLINT_AND_STEEL
                || item.getEnchantment(CloudEnchantmentInstance.ID_FIRE_ASPECT) != null) {
            if (!(this.isLit(block))) {
                this.toggleFire(block);
            }
            return true;
        } else if (item.isShovel()) {
            if (this.isLit(block)) {
                this.toggleFire(block);
            }
            return true;
        } else if (item instanceof ItemEdibleBehavior) {
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

            if (blockEntity instanceof Campfire) {
                Campfire fire = (Campfire) blockEntity;

                if (fire.putItemInFire(item)) {
                    if (player != null && player.isSurvival()) {
                        item.decrementCount();
                        if (item.getCount() <= 0) {
                            item = ItemStack.get(BlockTypes.AIR);
                        }
                        player.getInventory().setItemInHand(item);
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
        if (type == Level.BLOCK_UPDATE_NORMAL) {
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
