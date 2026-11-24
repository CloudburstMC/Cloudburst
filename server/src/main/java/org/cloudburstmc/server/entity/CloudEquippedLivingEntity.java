package org.cloudburstmc.server.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.ArmorComponent;
import org.cloudburstmc.api.item.component.DeathProtectionComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.view.CloudArmorView;
import org.cloudburstmc.server.container.view.CloudOffhandView;
import org.cloudburstmc.server.enchantment.EquipmentEnchantmentEffects;
import org.cloudburstmc.server.entity.data.CloudEquipmentData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.function.DoubleUnaryOperator;

/**
 * Shared armor and offhand behavior for living entities that carry equipment.
 */
public abstract class CloudEquippedLivingEntity extends CloudLivingEntity {

    protected final CloudArmorView armor = new CloudArmorView(this, new CloudContainer(4));
    protected final CloudOffhandView offhand = new CloudOffhandView(this, new CloudContainer(1));

    private final EquipmentEnchantmentEffects locationEffects = new EquipmentEnchantmentEffects();

    protected CloudEquippedLivingEntity(EntityType<?> type, Location location, CloudEquipmentData equipmentData) {
        super(type, location, equipmentData);
        equipmentData.setArmor(this.armor.getContainer());
        equipmentData.setOffhand(this.offhand.getContainer());
    }

    public ArmorView getArmor() {
        return this.armor;
    }

    public OffhandView getOffhand() {
        return this.offhand;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (this.isAlive() && tickDiff > 0) {
            this.updateEnchantmentLocationEffects();
        }
        return updated;
    }

    public void updateEnchantmentLocationEffects() {
        if (this.isAlive()) {
            this.locationEffects.update(this, this.getArmor());
        }
    }

    @Override
    protected float getHelmetDamageMultiplier(DamageSource source) {
        if (source.getDamageType().is(DamageTypeTags.DAMAGES_HELMET) && !this.getArmor().getHelmet().isEmpty()) {
            return 0.75f;
        }

        return 1;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        if (super.isDamageImmune(source)) {
            return true;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }

            ItemStack item = this.getArmor().getItem(slot.getArmorSlot());
            if (CloudEnchantmentRegistry.get().grantsDamageImmunity(item, slot, source)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected DoubleUnaryOperator createDamageReduction(DamageSource source) {
        DoubleUnaryOperator armorReduction = source.getDamageType().is(DamageTypeTags.BYPASSES_ARMOR) ? damage -> damage : this.createArmorReduction(source);
        DoubleUnaryOperator effectReduction = super.createDamageReduction(source);

        boolean bypassesProtection = source.getDamageType().is(DamageTypeTags.BYPASSES_EFFECTS) || source.getDamageType().is(DamageTypeTags.BYPASSES_ENCHANTMENTS);
        float enchantmentMultiplier = bypassesProtection ? 1 : this.getEnchantmentDamageMultiplier(source);

        return damage -> {
            double damageAfterArmor = armorReduction.applyAsDouble(damage);
            return Math.max(0, effectReduction.applyAsDouble(damageAfterArmor) * enchantmentMultiplier);
        };
    }

    private DoubleUnaryOperator createArmorReduction(DamageSource source) {
        double armorPoints = 0;
        double toughness = 0;

        ArmorView armorView = this.getArmor();
        for (int armorSlot = 0; armorSlot < armorView.size(); armorSlot++) {
            ItemStack armor = armorView.getItem(armorSlot);
            ArmorComponent armorComponent = CloudItemRegistry.get().getComponent(armor.getType(), ItemBehaviors.ARMOR);
            if (armorComponent != null) {
                armorPoints += armorComponent.defense();
                toughness += armorComponent.toughness();
            }
        }

        double toughnessFactor = 2 + toughness / 4;
        double totalArmor = armorPoints;
        double minimumArmor = Math.min(20, totalArmor * 0.2);

        return damage -> {
            double effectiveArmor = Math.clamp(totalArmor - damage / toughnessFactor, minimumArmor, 20);
            float effectiveness = CloudEnchantmentRegistry.get().modifyArmorEffectiveness(source.getWeaponItem(), (float) (effectiveArmor / 25));
            return damage * (1 - effectiveness);
        };
    }

    private float getEnchantmentDamageMultiplier(DamageSource source) {
        float protection = 0;
        ArmorView armorView = this.getArmor();
        for (int slot = 0; slot < armorView.size(); slot++) {
            protection += CloudEnchantmentRegistry.get().getDamageProtection(armorView.getItem(slot), source);
        }

        return 1 - Math.clamp(protection, 0, 20) * 0.04f;
    }

    @Override
    protected float getKnockbackResistance() {
        float resistance = 0;
        ArmorView armorView = this.getArmor();

        for (int armorSlot = 0; armorSlot < armorView.size(); armorSlot++) {
            ItemStack armor = armorView.getItem(armorSlot);
            ArmorComponent armorComponent = CloudItemRegistry.get().getComponent(armor.getType(), ItemBehaviors.ARMOR);
            if (armorComponent != null) {
                resistance += armorComponent.knockbackResistance();
            }
        }

        return Math.clamp(resistance, 0, 1);
    }

    @Override
    public float getExplosionKnockbackResistance() {
        float resistance = 0;
        ArmorView armorView = this.getArmor();
        for (int slot = 0; slot < armorView.size(); slot++) {
            resistance += CloudEnchantmentRegistry.get().getExplosionKnockbackResistance(armorView.getItem(slot));
        }
        return Math.clamp(resistance, 0, 1);
    }

    @Override
    protected float getBurningTimeReduction() {
        float reduction = 0;
        ArmorView armorView = this.getArmor();
        for (int slot = 0; slot < armorView.size(); slot++) {
            reduction += CloudEnchantmentRegistry.get().getBurningTimeReduction(armorView.getItem(slot));
        }
        return Math.clamp(reduction, 0, 1);
    }

    @Override
    protected void applyDamageEffects(CloudEntityDamageEvent event, float damageBeforeReductions) {
        super.applyDamageEffects(event, damageBeforeReductions);
        if (event.getDamageType().is(DamageTypeTags.DAMAGES_HELMET)) {
            this.hurtHelmet(event.getHelmetDamage());
        }

        if (!event.getDamageType().is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(damageBeforeReductions);
        }

        Entity attacker = event.getDamageSource().getCausingEntity();
        if (attacker == null) {
            return;
        }

        for (int slot = 0; slot < this.getArmor().size(); slot++) {
            ItemStack original = this.getArmor().getItem(slot);
            if (original.isEmpty()) {
                continue;
            }

            ItemStack replacement = CloudEnchantmentRegistry.get().applyPostHurtEffects(original, this, attacker);
            if (!replacement.equals(original) && this.getArmor().getItem(slot).equals(original)) {
                this.getArmor().setItem(slot, replacement);
            }
        }
    }

    protected void hurtHelmet(float damage) {
    }

    protected void hurtArmor(float damage) {
    }

    @Override
    protected @Nullable DeathProtectionUse findDeathProtection() {
        ItemStack item = this.getOffhand().getOffhandItem();
        if (item.isEmpty()) {
            return null;
        }

        DeathProtectionComponent protection = CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.DEATH_PROTECTION);
        if (protection == null) {
            return null;
        }

        return new DeathProtectionUse(EquipmentSlot.OFF_HAND, item, protection, replacement -> {
            if (this.getOffhand().getOffhandItem().equals(item)) {
                this.getOffhand().setOffhandItem(replacement);
            }
        });
    }
}
