package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorMagma extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public int getLightLevel() {
        return 3;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public void onEntityCollide(Entity entity) {
        if (!entity.hasEffect(Effect.FIRE_RESISTANCE)) {
            if (entity instanceof Player) {
                Player p = (Player) entity;
                if (!p.isCreative() && !p.isSpectator() && !p.isSneaking()) {
                    entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.LAVA, 1));
                }
            } else {
                entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.LAVA, 1));
            }
        }
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.NETHERRACK_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

}
