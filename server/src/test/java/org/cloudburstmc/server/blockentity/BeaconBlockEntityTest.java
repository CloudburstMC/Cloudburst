package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.potion.EffectTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeaconBlockEntityTest {

    @Test
    void primaryPowersRequireTheirPyramidTier() {
        assertFalse(BeaconBlockEntity.supportsSelection(0, EffectTypes.SPEED, null));
        assertTrue(BeaconBlockEntity.supportsSelection(1, EffectTypes.SPEED, null));
        assertFalse(BeaconBlockEntity.supportsSelection(1, EffectTypes.JUMP_BOOST, null));
        assertTrue(BeaconBlockEntity.supportsSelection(2, EffectTypes.JUMP_BOOST, null));
        assertFalse(BeaconBlockEntity.supportsSelection(2, EffectTypes.STRENGTH, null));
        assertTrue(BeaconBlockEntity.supportsSelection(3, EffectTypes.STRENGTH, null));
    }

    @Test
    void fullPyramidAllowsRegenerationOrAnUpgradeOfThePrimaryPower() {
        assertFalse(BeaconBlockEntity.supportsSelection(3, EffectTypes.HASTE, EffectTypes.REGENERATION));
        assertTrue(BeaconBlockEntity.supportsSelection(4, EffectTypes.HASTE, EffectTypes.REGENERATION));
        assertTrue(BeaconBlockEntity.supportsSelection(4, EffectTypes.HASTE, EffectTypes.HASTE));
        assertFalse(BeaconBlockEntity.supportsSelection(4, EffectTypes.HASTE, EffectTypes.SPEED));
        assertFalse(BeaconBlockEntity.supportsSelection(4, EffectTypes.REGENERATION, null));
        assertFalse(BeaconBlockEntity.supportsSelection(4, null, EffectTypes.REGENERATION));
    }
}
