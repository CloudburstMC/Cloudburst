package org.cloudburstmc.server.item;

import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.ArmorComponent;
import org.cloudburstmc.api.item.component.FloatItemHandler;
import org.cloudburstmc.api.item.component.IntItemHandler;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatItemBehaviorsTest {

    private static final CloudItemRegistry ITEMS = CloudItemRegistry.get();

    @Test
    void exposesVanillaArmorProtection() {
        assertEquals(new ArmorComponent(1, 0, 0), armor(ItemTypes.LEATHER_HELMET));
        assertEquals(new ArmorComponent(8, 2, 0), armor(ItemTypes.DIAMOND_CHESTPLATE));
        assertEquals(new ArmorComponent(3, 3, 0.1f), armor(ItemTypes.NETHERITE_BOOTS));
    }

    @Test
    void exposesVanillaMeleeDamage() {
        assertEquals(4, attackDamage(ItemTypes.WOODEN_SWORD));
        assertEquals(5, attackDamage(ItemTypes.DIAMOND_PICKAXE));
        assertEquals(10, attackDamage(ItemTypes.NETHERITE_AXE));
        assertEquals(9, attackDamage(ItemTypes.TRIDENT));
        assertEquals(DamageTypes.SPEAR, ITEMS.requireComponent(ItemTypes.DIAMOND_SPEAR, ItemBehaviors.ATTACK_DAMAGE_TYPE));
    }

    @Test
    void damagesOnlyItemsConfiguredForMeleeAttacks() {
        assertEquals(1, attackDurabilityDamage(ItemTypes.DIAMOND_SWORD));
        assertEquals(1, attackDurabilityDamage(ItemTypes.DIAMOND_PICKAXE));
        assertEquals(0, attackDurabilityDamage(ItemTypes.ELYTRA));
        assertEquals(0, attackDurabilityDamage(ItemTypes.BOW));
    }

    private static ArmorComponent armor(ItemType type) {
        return ITEMS.requireComponent(type, ItemBehaviors.ARMOR);
    }

    private static float attackDamage(ItemType type) {
        FloatItemHandler component = ITEMS.requireComponent(type, ItemBehaviors.GET_ATTACK_DAMAGE);
        return component.execute(ItemStack.from(type));
    }

    private static int attackDurabilityDamage(ItemType type) {
        IntItemHandler component = ITEMS.requireComponent(type, ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE);
        return component.execute(ItemStack.from(type));
    }
}
