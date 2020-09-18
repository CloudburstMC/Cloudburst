package org.cloudburstmc.server.item.behavior;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.ToolTypes;
import org.cloudburstmc.server.item.data.Damageable;

import java.util.Random;

import static org.cloudburstmc.server.block.BlockTypes.DIRT;
import static org.cloudburstmc.server.block.BlockTypes.GRASS;
import static org.cloudburstmc.server.item.ItemTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@RequiredArgsConstructor
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

    protected final ToolType toolType;
    protected final TierType tierType;

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public ItemStack useOn(ItemStack item, Block block) {
        if (this.isUnbreakable(item) || isDurable(item)) {
            return item;
        }

        val state = block.getState();
        val behavior = state.getBehavior();
        var damage = item.getMetadata(Damageable.class);

        if (damage != null) {
            val itemBehavior = item.getBehavior();
            if (behavior.getToolType() == itemBehavior.getToolType(item)) {
                return item.withData(damage.damage());
            }

            if (itemBehavior.isShears() && behavior.getBreakTime(state, item, null) > 0) {
                return item.withData(damage.damage(2));
            }

            if (itemBehavior.isHoe()) {
                if (state.getType() == GRASS || state.getType() == DIRT) {
                    return item.withData(damage.damage());
                }
            } else {
                return item.withData(damage.damage());
            }
        }

        return item;
    }

    @Override
    public ItemStack useOn(ItemStack item, Entity entity) {
        if (this.isUnbreakable(item) || isDurable(item)) {
            return item;
        }

        var damage = item.getMetadata(Damageable.class);

        if (damage != null) {
            if ((entity != null) && !this.isSword()) {
                damage = damage.damage(2);
            } else {
                damage = damage.damage();
            }

            return item.withData(damage);
        }

        return item;
    }

    private boolean isDurable(ItemStack item) {
        if (!item.hasEnchantments()) {
            return false;
        }

        EnchantmentInstance durability = item.getEnchantment(EnchantmentTypes.DURABILITY);
        return durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= new Random().nextInt(100);
    }

    @Override
    public boolean isUnbreakable(ItemStack item) {
        val damage = item.getMetadata(Damageable.class);
        return damage != null && damage.isUnbreakable();
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
        return this.toolType == ToolTypes.SHEARS;
    }

    @Override
    public boolean isTool(ItemStack item) {
        val type = item.getType();
        return type == FLINT_AND_STEEL || type == SHEARS || type == BOW ||
                this.isPickaxe() || this.isAxe() || this.isShovel() || this.isSword() || this.isHoe();
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        return item.getBehavior().getTier(item).getToolEnchantAbility();
    }
}
