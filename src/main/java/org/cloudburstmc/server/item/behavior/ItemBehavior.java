package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemBehavior {
    boolean canBeActivated();

    boolean canPlaceOn(ItemStack item, Identifier identifier);

    boolean canDestroy(ItemStack item, Identifier identifier);

    boolean canBePlaced(ItemStack item);

    BlockState getBlock(ItemStack item);

    int getMaxStackSize(ItemStack item);

    int getFuelTime(ItemStack item);

    ItemStack useOn(ItemStack item, Entity entity);

    ItemStack useOn(ItemStack item, Block block);

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
