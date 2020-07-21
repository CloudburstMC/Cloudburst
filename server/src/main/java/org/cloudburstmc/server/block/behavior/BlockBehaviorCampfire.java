package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Campfire;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemEdible;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.Faceable;

/**
 * @author Sleepybear
 */
public class BlockBehaviorCampfire extends BlockBehaviorSolid implements Faceable {

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
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & CAMPFIRE_FACING_MASK);
    }

    public boolean isLit() {
        return (this.getMeta() & CAMPFIRE_LIT_MASK) == 0;
    }

    public void toggleFire() {
        if (!this.isLit() && isWaterlogged()) return;
        this.setMeta(this.getMeta() ^ CAMPFIRE_LIT_MASK);
        getLevel().setBlockDataAt(this.getX(), this.getY(), this.getZ(), this.getMeta());
        Campfire cf = (Campfire) getLevel().getBlockEntity(this.getPosition());
        if (cf != null) getLevel().getBlockEntity(this.getPosition()).scheduleUpdate();
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (!blockState.canBeReplaced()) return false;
        if (blockState.down().getId() == BlockTypes.CAMPFIRE) {
            return false;
        }
        this.setMeta(player.getHorizontalFacing().getOpposite().getHorizontalIndex() & CAMPFIRE_FACING_MASK);
        if ((blockState.getId() == BlockTypes.WATER || blockState.getId() == BlockTypes.FLOWING_WATER) && blockState.getMeta() == 0) {
            this.setMeta(this.getMeta() + CAMPFIRE_LIT_MASK);
            getLevel().setBlock(blockState.getX(), blockState.getY(), blockState.getZ(), 1, blockState.clone(), true, false);
        }

        if (getLevel().setBlock(blockState.getPosition(), this, true, true)) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.CAMPFIRE, this.getChunk(), this.getPosition());
            return true;
        }
        return false;
    }

    @Override
    public int getLightLevel() {
        return isLit() ? 15 : 0;
    }

    @Override
    public boolean canBeActivated() {
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
    public boolean onActivate(Block block, Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.getEnchantment(Enchantment.ID_SILK_TOUCH) != null) {
            return super.getDrops(hand);
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.FLINT_AND_STEEL
                || item.getEnchantment(Enchantment.ID_FIRE_ASPECT) != null) {
            if (!(this.isLit())) {
                this.toggleFire();
            }
            return true;
        } else if (item.isShovel()) {
            if (this.isLit()) {
                this.toggleFire();
            }
            return true;
        } else if (item instanceof ItemEdible) {
            Campfire fire = (Campfire) getLevel().getBlockEntity(this.getPosition());
            if (fire.putItemInFire(item)) {
                if (player != null && player.isSurvival()) {
                    item.decrementCount();
                    if (item.getCount() <= 0) {
                        item = Item.get(BlockTypes.AIR);
                    }
                    player.getInventory().setItemInHand(item);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        if (this.isLit() && entity instanceof EntityLiving) {
            entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.FIRE_TICK, 1.0f));
        } else if (!this.isLit() && entity.isOnFire()) {
            this.toggleFire();
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.isLit()) {
                if (this.isWaterlogged()) {
                    this.toggleFire();
                    return type;
                }

                BlockState up = this.up();
                if (up.getId() == BlockTypes.WATER || up.getId() == BlockTypes.FLOWING_WATER) {
                    this.toggleFire();
                    return type;
                }
            }
            return type;
        }

        return 0;
    }
}
