package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.BlockItemStack;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
public abstract class CloudItemBehavior implements ItemBehavior {

    @Override
    public boolean canBeActivated() {
        return false;
    }

    @Override
    public boolean canPlaceOn(ItemStack item, Identifier identifier) {
        return item.getCanPlaceOn().contains(identifier);
    }

    @Override
    public boolean canDestroy(ItemStack item, Identifier identifier) {
        return item.getCanDestroy().contains(identifier);
    }

    @Override
    public final boolean canBePlaced(ItemStack item) {
        BlockState state = this.getBlock(item);

        if (state == null) {
            return false;
        }

        BlockBehavior behavior = state.getBehavior();
        return behavior.canBePlaced();
    }

    @Override
    public BlockState getBlock(ItemStack item) {
        if (item instanceof BlockItemStack) {
            return item.getBlockState();
        }

        val b = item.getType().getBlockType();
        return b != null ? CloudBlockRegistry.get().getBlock(b) : BlockStates.AIR;
    }

    @Override
    public int getMaxStackSize(ItemStack item) {
        return item.getType().getMaximumStackSize();
    }

    @Override
    public short getFuelTime(ItemStack item) {
        return item.getType().getFuelTime();
    }

    @Override
    public ItemStack useOn(ItemStack item, Entity entity) {
        return null;
    }

    @Override
    public ItemStack useOn(ItemStack item, BlockState block) {
        return null;
    }

    @Override
    public boolean isTool(ItemStack item) {
        return false;
    }

    @Override
    public int getMaxDurability() {
        return -1;
    }

    @Override
    public TierType getTier(ItemStack item) {
        return item.getType().getTierType();
    }

    @Override
    public ToolType getToolType(ItemStack item) {
        return item.getType().getToolType();
    }

    @Override
    public boolean isPickaxe() {
        return false;
    }

    @Override
    public boolean isAxe() {
        return false;
    }

    @Override
    public boolean isSword() {
        return false;
    }

    @Override
    public boolean isShovel() {
        return false;
    }

    @Override
    public boolean isHoe() {
        return false;
    }

    @Override
    public boolean isShears() {
        return false;
    }

    @Override
    public boolean isArmor() {
        return false;
    }

    @Override
    public boolean isHelmet() {
        return false;
    }

    @Override
    public boolean isChestplate() {
        return false;
    }

    @Override
    public boolean isLeggings() {
        return false;
    }

    @Override
    public boolean isBoots() {
        return false;
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        return 0;
    }

    @Override
    public int getAttackDamage(ItemStack item) {
        return item.getType().getAttackDamage();
    }

    @Override
    public int getArmorPoints(ItemStack item) {
        return item.getType().getArmorPoints();
    }

    @Override
    public int getToughness(ItemStack item) {
        return item.getType().getToughness();
    }

    @Override
    public boolean isUnbreakable(ItemStack item) {
        return false;
    }

    @Override
    public ItemStack onUse(ItemStack item, int ticksUsed, Player player) {
        return null;
    }

    @Override
    public ItemStack onRelease(ItemStack item, int ticksUsed, Player player) {
        return null;
    }


    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block blockState, Block blockState1, Direction direction, Vector3f vector3f, Level level) {
        return null;
    }

    /**
     * Called when a player uses the item on air, for example throwing a projectile.
     * Returns whether the item was changed, for example count decrease or durability change.
     *
     * @param item
     * @param directionVector direction
     * @param player          player
     * @return item changed
     */
    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        return false;
    }
}
