package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.ArmorComponent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreatureDamageTest {

    @Test
    void equippedMobsReceiveArmorDefenseAndHonorArmorBypasses() {
        ArmoredMob mob = new ArmoredMob();
        mob.getArmor().setChestplate(ItemStack.builder().itemType(ItemTypes.DIAMOND_CHESTPLATE).build());

        assertEquals(8.4f, mob.createDamageEvent(DamageSource.of(DamageTypes.PLAYER_ATTACK), 10).getFinalDamage(), 1.0e-5f);
        assertEquals(10, mob.createDamageEvent(DamageSource.of(DamageTypes.MAGIC), 10).getFinalDamage());
    }

    @Test
    void ordinaryMobArmorDoesNotInheritPlayerDurabilityWear() {
        ArmoredMob mob = new ArmoredMob();
        ItemStack armor = ItemStack.builder().itemType(ItemTypes.DIAMOND_CHESTPLATE).build();
        mob.getArmor().setChestplate(armor);
        CloudEntityDamageEvent event = mob.createDamageEvent(DamageSource.of(DamageTypes.PLAYER_ATTACK), 10);

        mob.applyDamageEffects(event, event.getDamageBeforeReductions());

        assertEquals(armor, mob.getArmor().getChestplate());
    }

    @Test
    void largePluginArmorValuesRemainBounded() {
        CloudItemRegistry registry = CloudItemRegistry.get();
        ArmorComponent original = registry.requireComponent(ItemTypes.DIAMOND_CHESTPLATE, ItemBehaviors.ARMOR);
        try {
            registry.configure(ItemTypes.DIAMOND_CHESTPLATE)
                    .set(ItemBehaviors.ARMOR, new ArmorComponent(Integer.MAX_VALUE, Float.MAX_VALUE, 0));
            ArmoredMob mob = new ArmoredMob();
            mob.getArmor().setChestplate(ItemStack.builder().itemType(ItemTypes.DIAMOND_CHESTPLATE).build());

            assertEquals(2, mob.createDamageEvent(DamageSource.of(DamageTypes.PLAYER_ATTACK), 10).getFinalDamage(), 1.0e-5f);
        } finally {
            registry.configure(ItemTypes.DIAMOND_CHESTPLATE).set(ItemBehaviors.ARMOR, original);
        }
    }

    @Test
    void largeProtectionLevelsDoNotOverflowIntoNegativeProtection() {
        ArmoredMob mob = new ArmoredMob();
        mob.getArmor().setBoots(ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.FEATHER_FALLING, new Enchantment(EnchantmentTypes.FEATHER_FALLING, Integer.MAX_VALUE)))
                .build());

        assertEquals(2, mob.createDamageEvent(DamageSource.of(DamageTypes.FALL), 10).getFinalDamage(), 1.0e-5f);
    }

    @Test
    void equippedMobsCanUseOffhandDeathProtection() {
        ArmoredMob mob = new ArmoredMob();
        mob.getOffhand().setOffhandItem(ItemStack.builder().itemType(ItemTypes.TOTEM_OF_UNDYING).build());

        EntityLiving.DeathProtectionUse protection = mob.findDeathProtection();
        assertNotNull(protection);
        assertEquals(EquipmentSlot.OFF_HAND, protection.hand());
        protection.update().accept(ItemStack.EMPTY);
        assertEquals(ItemStack.EMPTY, mob.getOffhand().getOffhandItem());
    }

    @Test
    void blastProtectionAddsResistanceAcrossArmorSlotsAndCapsAtOne() {
        ArmoredMob mob = new ArmoredMob();
        mob.getArmor().setBoots(ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.BLAST_PROTECTION, new Enchantment(EnchantmentTypes.BLAST_PROTECTION, 2)))
                .build());
        mob.getArmor().setChestplate(ItemStack.builder().itemType(ItemTypes.DIAMOND_CHESTPLATE)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.BLAST_PROTECTION, new Enchantment(EnchantmentTypes.BLAST_PROTECTION, 4)))
                .build());

        assertEquals(0.9f, mob.getExplosionKnockbackResistance(), 1.0e-6f);
        mob.getArmor().setBoots(ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.BLAST_PROTECTION, new Enchantment(EnchantmentTypes.BLAST_PROTECTION, Integer.MAX_VALUE)))
                .build());
        assertEquals(1, mob.getExplosionKnockbackResistance());
    }

    @Test
    void fireProtectionAddsAcrossSlotsWithoutRoundingToWholeSeconds() {
        ArmoredMob mob = new ArmoredMob();
        mob.getArmor().setBoots(ItemStack.builder().itemType(ItemTypes.DIAMOND_BOOTS)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.FIRE_PROTECTION, new Enchantment(EnchantmentTypes.FIRE_PROTECTION, 1)))
                .build());
        mob.getArmor().setChestplate(ItemStack.builder().itemType(ItemTypes.DIAMOND_CHESTPLATE)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.FIRE_PROTECTION, new Enchantment(EnchantmentTypes.FIRE_PROTECTION, 2)))
                .build());

        mob.setOnFire(4);
        assertEquals(44, mob.getFireTicks());
        mob.setOnFire(1);
        assertEquals(44, mob.getFireTicks());
    }

    @Test
    void ignitionDurationCannotOverflowOrAcceptNegativeTime() {
        ArmoredMob mob = new ArmoredMob();
        mob.setOnFire(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, mob.getFireTicks());
        assertThrows(IllegalArgumentException.class, () -> mob.setOnFire(-1));
    }

    private static class ArmoredMob extends EntityCreature {
        private ArmoredMob() {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }
    }
}
