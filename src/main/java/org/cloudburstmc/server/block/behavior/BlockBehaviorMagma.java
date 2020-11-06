package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorMagma extends BlockBehaviorSolid {

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


}
