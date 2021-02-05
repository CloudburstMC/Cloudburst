package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorMagma extends BlockBehaviorSolid {

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (!entity.hasEffect(CloudEffect.FIRE_RESISTANCE)) {
            if (entity instanceof CloudPlayer) {
                CloudPlayer p = (CloudPlayer) entity;
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
