package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorMagma extends BlockBehaviorSolid {

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (!entity.hasEffect(EffectTypes.FIRE_RESISTANCE)) {
            if (entity instanceof CloudPlayer p) {
                if (!p.isCreative() && !p.isSpectator() && !p.isSneaking() && p.getLevel().getGameRules().get(GameRules.FIRE_DAMAGE)) {
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
