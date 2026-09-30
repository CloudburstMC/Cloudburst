package org.cloudburstmc.server.potion;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbsorptionEffectTest {

    @Test
    void refreshingAbsorptionPublishesTheSameAmountWithoutStacking() {
        AbsorptionTarget target = new AbsorptionTarget();
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.ABSORPTION, 2400, 3));

        effect.onApplied(target, null);
        effect.onApplied(target, effect);
        effect.onApplied(target, effect);

        assertEquals(16, target.getAbsorption());
        assertEquals(3, target.absorptionUpdates);
    }

    @Test
    void refreshingRefillsDepletedAbsorptionButPreservesLargerAmounts() {
        AbsorptionTarget target = new AbsorptionTarget();
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.ABSORPTION, 2400, 3));

        target.setAbsorption(5.5f);
        effect.onApplied(target, effect);
        assertEquals(16, target.getAbsorption());

        target.setAbsorption(20);
        effect.onApplied(target, effect);
        assertEquals(20, target.getAbsorption());
    }

    @Test
    void removingADepletedEffectStillPublishesZeroAbsorption() {
        AbsorptionTarget target = new AbsorptionTarget();
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.ABSORPTION, 2400, 3));

        effect.onRemoved(target);

        assertEquals(0, target.getAbsorption());
        assertEquals(1, target.absorptionUpdates);
    }

    private static class AbsorptionTarget extends EntityLiving {
        private int absorptionUpdates;

        private AbsorptionTarget() {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public void setAbsorption(float absorption) {
            super.setAbsorption(absorption);
            this.absorptionUpdates++;
        }
    }
}
