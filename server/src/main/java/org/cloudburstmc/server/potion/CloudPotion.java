package org.cloudburstmc.server.potion;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.potion.PotionApplyEvent;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.api.potion.EffectTypes.HARMING;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudPotion extends Potion {

    private PotionType potion;

    public CloudPotion(PotionType type, boolean splash) {
        super(type, splash);
        this.potion = type;
    }

    @Override
    public Effect getEffect() {
        return new CloudEffect(potion.getType())
                .setDuration(potion.getDuration())
                .setAmplifier(potion.getLevel() - 1)
                .setAmbient(potion.isSplash());
    }

    @Override
    public void applyPotion(Entity entity) {
        applyPotion(entity, 0.5);
    }

    public void applyPotion(Entity entity, double health) {
        if (!(entity instanceof EntityLiving)) {
            return;
        }

        Effect applyEffect = this.getEffect();

        if (applyEffect == null) {
            return;
        }

        if (entity instanceof CloudPlayer) {
            if (!((CloudPlayer) entity).isSurvival() && !((CloudPlayer) entity).isAdventure() && applyEffect.isBad()) {
                return;
            }
        }

        PotionApplyEvent event = new PotionApplyEvent(this, applyEffect, entity);

        entity.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        applyEffect = event.getApplyEffect();

        if (potion.getType() == EffectTypes.HEALING) {
            if (entity.isUndead())
                entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.MAGIC, (float) (health * (double) (6 << (applyEffect.getAmplifier() + 1)))));
            else
                entity.heal(new EntityRegainHealthEvent(entity, (float) (health * (double) (4 << (applyEffect.getAmplifier() + 1))), EntityRegainHealthEvent.CAUSE_MAGIC));
        } else if (potion.getType() == HARMING) {
            if (entity.isUndead())
                entity.heal(new EntityRegainHealthEvent(entity, (float) (health * (double) (4 << (applyEffect.getAmplifier() + 1))), EntityRegainHealthEvent.CAUSE_MAGIC));
            else
                entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.MAGIC, (float) (health * (double) (6 << (applyEffect.getAmplifier() + 1)))));
        } else {
            int duration = (int) ((isSplash() ? health : 1) * (double) applyEffect.getDuration() + 0.5);
            applyEffect.setDuration(duration);
            entity.addEffect(applyEffect);
        }
    }
}
