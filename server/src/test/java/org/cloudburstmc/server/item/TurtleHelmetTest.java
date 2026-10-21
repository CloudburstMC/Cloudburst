package org.cloudburstmc.server.item;

import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.ArmorComponent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.item.component.ArmorItemHandlers;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurtleHelmetTest {

    @Test
    void refreshesWaterBreathingOnlyWhileWornWithEyesOutOfWater() {
        Wearer wearer = new Wearer();
        assertNull(ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, true));

        PotionEffect applied = ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, false);
        assertNotNull(applied);
        assertSame(EffectTypes.WATER_BREATHING, applied.getType());
        assertEquals(200, applied.getDuration());
        assertEquals(0, applied.getAmplifier());
        assertFalse(applied.isAmbient());
        assertFalse(applied.hasParticles());

        wearer.effect = applied.withDuration(123);
        assertNull(ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, true));
        assertEquals(123, assertInstanceOf(PotionEffect.class, wearer.effect).getDuration());

        wearer.helmet = ItemStack.EMPTY;
        assertNull(ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, false));
        assertEquals(123, assertInstanceOf(PotionEffect.class, wearer.effect).getDuration());

        wearer.helmet = ItemStack.from(ItemTypes.TURTLE_HELMET);
        PotionEffect refreshed = ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, false);
        assertNotNull(refreshed);
        assertEquals(200, refreshed.getDuration());
    }

    @Test
    void preservesLongerStrongerAndInfiniteWaterBreathing() {
        for (PotionEffect current : List.of(new PotionEffect(EffectTypes.WATER_BREATHING, 1200, 0),
                new PotionEffect(EffectTypes.WATER_BREATHING, 50, 1),
                new PotionEffect(EffectTypes.WATER_BREATHING, PotionEffect.INFINITE_DURATION, 0))) {
            Wearer wearer = new Wearer();
            wearer.effect = current;

            assertNull(ArmorItemHandlers.getTurtleHelmetEffect(wearer.player, false));
            assertSame(current, wearer.effect);
        }
    }

    @Test
    void usesVanillaArmorDurabilitySlotAndScuteRepairs() {
        CloudItemRegistry items = CloudItemRegistry.get();
        ItemStack helmet = ItemStack.from(ItemTypes.TURTLE_HELMET);
        ArmorComponent armor = items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.ARMOR);

        assertEquals(2, armor.defense());
        assertEquals(0, armor.toughness());
        assertEquals(275, items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.GET_MAX_DAMAGE).execute(helmet));
        assertEquals(1, items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.GET_MAX_STACK_SIZE).execute(helmet));
        assertSame(EquipmentSlot.HEAD, items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.GET_EQUIPMENT_SLOT).execute(helmet));
        assertTrue(items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.CAN_REPAIR_WITH).execute(helmet, ItemStack.from(ItemTypes.TURTLE_SCUTE)));
        assertFalse(items.requireComponent(ItemTypes.TURTLE_HELMET, ItemBehaviors.CAN_REPAIR_WITH).execute(helmet, ItemStack.from(ItemTypes.ARMADILLO_SCUTE)));
    }

    private static class Wearer {

        private ItemStack helmet = ItemStack.from(ItemTypes.TURTLE_HELMET);
        private PotionEffect effect;
        private final ArmorView armor = InterfaceProxy.create(ArmorView.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getHelmet")) {
                return this.helmet;
            }
            throw new AssertionError("Unexpected armor access: " + method);
        });
        private final Player player = InterfaceProxy.create(Player.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "isAlive" -> true;
            case "getArmor" -> this.armor;
            case "getPotionEffect" -> this.effect;
            default -> throw new AssertionError("Unexpected player access: " + method);
        });
    }
}
