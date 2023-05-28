package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;

public interface ItemBehavior {
    boolean canBeActivated();

    boolean canPlaceOn(ItemStack item, Identifier identifier);

    boolean canDestroy(ItemStack item, Identifier identifier);

    boolean canBePlaced(ItemStack item);

    BlockState getBlock(ItemStack item);

    int getMaxStackSize(ItemStack item);

    short getFuelTime(ItemStack item);

    ItemStack useOn(ItemStack item, Entity entity);

    ItemStack useOn(ItemStack item, BlockState block);

    boolean isTool(ItemStack item);

    int getMaxDurability();

    TierType getTier(ItemStack item);

    ToolType getToolType(ItemStack item);

    boolean isPickaxe();

    boolean isAxe();

    boolean isSword();

    boolean isShovel();

    boolean isHoe();

    boolean isShears();

    boolean isArmor();

    boolean isHelmet();

    boolean isChestplate();

    boolean isLeggings();

    boolean isBoots();

    ToolType getToolType(ItemType type);

    TierType getTierType(ItemType type);

    int getEnchantAbility(ItemStack item);

    int getAttackDamage(ItemStack item);

    int getArmorPoints(ItemStack item);

    int getToughness(ItemStack item);

    boolean isUnbreakable(ItemStack item);

    ItemStack onUse(ItemStack item, int ticksUsed, Player player);

    ItemStack onRelease(ItemStack item, int ticksUsed, Player player);

    ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level);

    boolean onClickAir(ItemStack item, Vector3f directionVector, Player player);
}
