package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


class WeaponDamageSourceTest {

    @Test
    void breachReducesArmorEffectivenessWithoutMakingItNegative() {
        ItemStack weapon = ItemStack.builder().itemType(ItemTypes.MACE)
                .setData(ItemDataComponents.ENCHANTMENTS, Map.of(EnchantmentTypes.BREACH, new Enchantment(EnchantmentTypes.BREACH, 4)))
                .build();
        DamageSource source = DamageSource.builder(DamageTypes.MACE_SMASH).weaponItem(weapon).build();

        assertEquals(0.2f, CloudEnchantmentRegistry.get().modifyArmorEffectiveness(source.getWeaponItem(), 0.8f), 1.0e-6f);
        assertEquals(0, CloudEnchantmentRegistry.get().modifyArmorEffectiveness(source.getWeaponItem(), 0.1f));
        assertEquals(0.8f, CloudEnchantmentRegistry.get().modifyArmorEffectiveness(ItemStack.EMPTY, 0.8f));
    }

    @Test
    void weaponContextIsPartOfTheDamageSourceValue() {
        ItemStack weapon = ItemStack.builder().itemType(ItemTypes.MACE).build();
        DamageSource armed = DamageSource.builder(DamageTypes.MACE_SMASH).weaponItem(weapon).build();
        DamageSource same = DamageSource.builder(DamageTypes.MACE_SMASH).weaponItem(weapon).build();
        DamageSource unarmed = DamageSource.of(DamageTypes.MACE_SMASH);

        assertEquals(armed, same);
        assertEquals(armed.hashCode(), same.hashCode());
        assertNotEquals(armed, unarmed);
    }
}
