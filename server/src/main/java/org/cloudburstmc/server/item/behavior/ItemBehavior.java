package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Optional;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
public abstract class ItemBehavior implements Cloneable {

    public boolean canBeActivated(ItemStack item) {
        return false;
    }

    public boolean canPlaceOn(ItemStack item, Identifier type) {
        return item.getCanPlaceOn().contains(type);
    }

    public boolean canDestroy(ItemStack item, Identifier type) {
        return item.getCanDestroy().contains(type);
    }

    public final boolean canBePlaced(ItemStack item) {
        BlockState state = this.getBlock(item);
        BlockBehavior behavior = state.getBehavior();
        return behavior.canBePlaced();
    }

    public BlockState getBlock(ItemStack item) {
        val state = BlockRegistry.get().getBlock(item.getType().getId());

        return BlockStates.AIR;
    }

    public int getMaxStackSize(ItemStack item) {
        return item.getType().getMaximumStackSize();
    }

    final public int getFuelTime(ItemStack item) {
//        if (!Fuel.duration.containsKey(id)) {
//            return null;
//        }
//        if (this.id != BUCKET || this.meta == 10) {
//            return Fuel.duration.get(this.id);
//        }
        return item.getType().getFuelTime();
    }

    public boolean useOn(ItemStack item, Entity entity) {
        return false;
    }

    public boolean useOn(ItemStack item, Block block) {
        return false;
    }

    public boolean isTool(ItemStack item) {
        return item.getType().getToolType().isPresent();
    }

    public int getMaxDurability(ItemStack item) {
        return item.getType().getDurability();
    }

    public Optional<TierType> getTier(ItemStack item) {
        return item.getType().getTierType();
    }

    public int getEnchantAbility(ItemStack item) {
        return 0;
    }

    public int getAttackDamage(ItemStack item) {
        return item.getType().getAttackDamage();
    }

    public int getArmorPoints(ItemStack item) {
        return item.getType().getArmorPoints();
    }

    public int getToughness(ItemStack item) {
        return item.getType().getToughness();
    }

    public boolean onUse(ItemStack item, Player player, int ticksUsed) {
        return false;
    }

    public boolean onRelease(ItemStack item, Player player, int ticksUsed) {
        return false;
    }

    public boolean onActivate(ItemStack item, Level level, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        return false;
    }

    /**
     * Called when a player uses the item on air, for example throwing a projectile.
     * Returns whether the item was changed, for example count decrease or durability change.
     *
     * @param player          player
     * @param directionVector direction
     * @return item changed
     */
    public boolean onClickAir(ItemStack item, Player player, Vector3f directionVector) {
        return false;
    }
}
