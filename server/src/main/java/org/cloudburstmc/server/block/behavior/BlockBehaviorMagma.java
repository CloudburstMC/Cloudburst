package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorMagma extends BlockBehaviorSolid {

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }


    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public int getLightLevel(Block block) {
        return 3;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (!entity.hasEffect(Effect.FIRE_RESISTANCE)) {
            if (entity instanceof Player) {
                Player p = (Player) entity;
                if (!p.isCreative() && !p.isSpectator() && !p.isSneaking()) {
                    entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.LAVA, 1));
                }
            } else {
                entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.LAVA, 1));
            }
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

}
