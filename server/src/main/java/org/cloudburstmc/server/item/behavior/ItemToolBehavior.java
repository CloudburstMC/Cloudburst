package org.cloudburstmc.server.item.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;

import java.util.Random;

import static org.cloudburstmc.server.block.BlockIds.DIRT;
import static org.cloudburstmc.server.block.BlockIds.GRASS;
import static org.cloudburstmc.server.item.ItemTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ItemToolBehavior extends CloudItemBehavior {

    public static final int TYPE_NONE = 0;
    public static final int TYPE_SWORD = 1;
    public static final int TYPE_SHOVEL = 2;
    public static final int TYPE_PICKAXE = 3;
    public static final int TYPE_AXE = 4;
    public static final int TYPE_SHEARS = 5;
    /**
     * Same breaking speed independent of the tool.
     */
    public static final int TYPE_HANDS_ONLY = 6;

    public static final int DURABILITY_WOODEN = 60;
    public static final int DURABILITY_GOLD = 33;
    public static final int DURABILITY_STONE = 132;
    public static final int DURABILITY_IRON = 251;
    public static final int DURABILITY_DIAMOND = 1562;
    public static final int DURABILITY_NETHERITE = 2031;
    public static final int DURABILITY_FLINT_STEEL = 65;
    public static final int DURABILITY_SHEARS = 239;
    public static final int DURABILITY_BOW = 385;
    public static final int DURABILITY_TRIDENT = 251;
    public static final int DURABILITY_FISHING_ROD = 65;

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public boolean useOn(ItemStack item, Block block) {
        if (this.isUnbreakable(item) || isDurable()) {
            return true;
        }

        val state = block.getState();
        val behavior = state.getBehavior();

        if (behavior.getToolType() == ItemToolBehavior.TYPE_PICKAXE && this.isPickaxe() ||
                behavior.getToolType() == ItemToolBehavior.TYPE_SHOVEL && this.isShovel() ||
                behavior.getToolType() == ItemToolBehavior.TYPE_AXE && this.isAxe() ||
                behavior.getToolType() == ItemToolBehavior.TYPE_SWORD && this.isSword() ||
                behavior.getToolType() == ItemToolBehavior.TYPE_SHEARS && this.isShears()
        ) {
            this.setMeta(getMeta() + 1);
        } else if (!this.isShears() && behavior.getBreakTime(this) > 0) {
            this.setMeta(getMeta() + 2);
        } else if (this.isHoe()) {
            if (state.getType() == GRASS || state.getType() == DIRT) {
                this.setMeta(getMeta() + 1);
            }
        } else {
            this.setMeta(getMeta() + 1);
        }
        return true;
    }

    @Override
    public boolean useOn(ItemStack item, Entity entity) {
        if (this.isUnbreakable(item) || isDurable(item)) {
            return true;
        }

        if ((entity != null) && !this.isSword()) {
            this.setMeta(getMeta() + 2);
        } else {
            this.setMeta(getMeta() + 1);
        }

        return true;
    }

    private boolean isDurable(ItemStack item) {
        if (!hasEnchantments()) {
            return false;
        }

        EnchantmentInstance durability = getEnchantment(CloudEnchantmentInstance.ID_DURABILITY);
        return durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= new Random().nextInt(100);
    }

    @Override
    public boolean isUnbreakable(ItemStack item) {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
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
        return (this.getId() == SHEARS);
    }

    @Override
    public boolean isTool(ItemStack item) {
        val type = item.getType();
        return type == FLINT_AND_STEEL || type == SHEARS || type == BOW ||
                this.isPickaxe() || this.isAxe() || this.isShovel() || this.isSword() || this.isHoe();
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        switch (this.getTier(item)) {
            case TIER_STONE:
                return 5;
            case TIER_WOODEN:
                return 15;
            case TIER_DIAMOND:
                return 10;
            case TIER_GOLD:
                return 22;
            case TIER_IRON:
                return 14;
        }

        return 0;
    }
}
