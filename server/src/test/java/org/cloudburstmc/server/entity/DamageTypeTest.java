package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.damage.DamageEffect;
import org.cloudburstmc.api.entity.damage.DamageScaling;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.damage.DeathMessageType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.network.VanillaTranslationKeys;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.api.entity.damage.DamageTypeTags.*;
import static org.junit.jupiter.api.Assertions.*;

class DamageTypeTest {

    @Test
    void exposesVanillaDamageBehavior() {
        assertAll(
                () -> assertEquals("death.attack.onFire", DamageTypes.ON_FIRE.getTranslationKey()),
                () -> assertEquals("death.attack.generic", DamageTypes.GENERIC_KILL.getTranslationKey()),
                () -> assertEquals(DamageEffect.BURNING, DamageTypes.ON_FIRE.getDamageEffect()),
                () -> assertEquals(DamageScaling.ALWAYS, DamageTypes.EXPLOSION.getDamageScaling()),
                () -> assertEquals(0.1f, DamageTypes.EXPLOSION.getExhaustion()),
                () -> assertEquals(DeathMessageType.FALL_VARIANTS, DamageTypes.FALL.getDeathMessageType())
        );
    }

    @Test
    public void usesAvailableVanillaDeathMessageTranslations() {
        for (var damageType : DamageTypes.values()) {
            assertTrue(VanillaTranslationKeys.contains(damageType.getTranslationKey()), damageType::toString);
        }
    }

    @Test
    void expandsInheritedDamageTypeTags() {
        assertAll(
                () -> assertTrue(DamageTypes.GENERIC.is(BYPASSES_SHIELD)),
                () -> assertTrue(DamageTypes.EXPLOSION.is(ALWAYS_HURTS_ENDER_DRAGONS)),
                () -> assertTrue(DamageTypes.PLAYER_ATTACK.is(CAN_BREAK_ARMOR_STAND)),
                () -> assertTrue(DamageTypes.CAMPFIRE.is(PANIC_CAUSES)),
                () -> assertFalse(DamageTypes.THORNS.is(NO_KNOCKBACK))
        );
    }

    @Test
    void mapsDamageEffectsToHurtEventData() {
        assertAll(
                () -> assertEquals(0, EntityLiving.getHurtEventData(DamageEffect.HURT)),
                () -> assertEquals(18, EntityLiving.getHurtEventData(DamageEffect.THORNS)),
                () -> assertEquals(7, EntityLiving.getHurtEventData(DamageEffect.BURNING)),
                () -> assertEquals(9, EntityLiving.getHurtEventData(DamageEffect.DROWNING)),
                () -> assertEquals(1, EntityLiving.getHurtEventData(DamageEffect.POKING)),
                () -> assertEquals(27, EntityLiving.getHurtEventData(DamageEffect.FREEZING))
        );
    }

    @Test
    void indexesBuiltInDamageTypesByIdentifier() {
        assertSame(DamageTypes.ON_FIRE, DamageTypes.get(Identifier.parse("on_fire")).orElseThrow());
        assertTrue(DamageTypes.values().contains(DamageTypes.ON_FIRE));
    }
}
