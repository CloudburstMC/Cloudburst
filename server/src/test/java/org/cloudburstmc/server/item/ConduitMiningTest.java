package org.cloudburstmc.server.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConduitMiningTest {

    private static final BlockState STONE = BlockTestRegistry.get().getBlock(BlockTypes.STONE);

    @Test
    void conduitPowerUsesTheStrongerHasteAmplifierWithoutStacking() {
        PotionEffect conduit = new PotionEffect(EffectTypes.CONDUIT_POWER, 260, 0);
        PotionEffect haste = new PotionEffect(EffectTypes.HASTE, 260, 2);

        assertEquals(1, speed(Map.of(), false), 0.00001f);
        assertEquals(1.2f, speed(Map.of(EffectTypes.CONDUIT_POWER, conduit), false), 0.00001f);
        assertEquals(1.6f, speed(Map.of(EffectTypes.HASTE, haste, EffectTypes.CONDUIT_POWER, conduit), false), 0.00001f);
        assertEquals(1.8f, speed(Map.of(EffectTypes.HASTE, haste, EffectTypes.CONDUIT_POWER, new PotionEffect(EffectTypes.CONDUIT_POWER, 260, 3)), false), 0.00001f);
        assertEquals(0.24f, speed(Map.of(EffectTypes.CONDUIT_POWER, conduit), true), 0.00001f);
    }

    private static float speed(Map<EffectType, PotionEffect> effects, boolean underwater) {
        Player player = InterfaceProxy.create(Player.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getPotionEffect" -> effects.get((EffectType) arguments[0]);
            case "isInsideOfWater" -> underwater;
            case "isOnGround" -> true;
            default -> throw new AssertionError("Unexpected player access: " + method);
        });

        return ToolUtils.getDestroySpeed(player, ItemStack.EMPTY, STONE);
    }
}
