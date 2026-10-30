package org.cloudburstmc.server.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Creature;
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
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.view.CloudArmorView;
import org.cloudburstmc.server.container.view.CloudOffhandView;
import org.cloudburstmc.server.enchantment.EquipmentEnchantmentEffects;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Abstract base class for all living creatures. Provides armor (4-slot) and offhand (1-slot) containers.
 */
public abstract class EntityCreature extends EntityLiving implements Creature {

    private final EquipmentEnchantmentEffects enchantmentEffects = new EquipmentEnchantmentEffects();

    protected final CloudArmorView armor = new CloudArmorView(this, new CloudContainer(4));
    protected final CloudOffhandView offhand = new CloudOffhandView(this, new CloudContainer(1));

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void applyData(EntityData data) {
        super.applyData(data);
        CreatureData creatureData = (CreatureData) data;
        if (creatureData.getOffhand() != null && !creatureData.getOffhand().isEmpty()) {
            this.offhand.setOffhandItem(ItemUtils.deserializeItem(creatureData.getOffhand().getFirst()));
        }
        if (creatureData.getArmor() != null && !creatureData.getArmor().isEmpty()) {
            CloudContainer container = this.armor.getContainer();
            List<NbtMap> items = creatureData.getArmor();
            for (int slot = 0; slot < 4 && slot < items.size(); ++slot) {
                container.setItem(slot, ItemUtils.deserializeItem(items.get(slot)));
            }
        }
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        CreatureData creatureData = (CreatureData) data;
        creatureData.setOffhand(List.of(ItemUtils.serializeItem(this.offhand.getOffhandItem(), 0)));

        List<NbtMap> armor = new ArrayList<>(this.armor.size());
        for (int i = 0; i < this.armor.size(); i++) {
            armor.add(ItemUtils.serializeItem(this.armor.getItem(i), i));
        }
        creatureData.setArmor(armor);
    }

    public ArmorView getArmor() {
        return armor;
    }

    public OffhandView getOffhand() {
        return offhand;
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
            this.enchantmentEffects.update(this, this.getArmor());
        }
    }

    @Override
    protected float getHelmetDamageMultiplier(DamageSource source) {
        return source.getDamageType().is(DamageTypeTags.DAMAGES_HELMET) && !this.getArmor().getHelmet().isEmpty() ? 0.75f : 1;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        if (super.isDamageImmune(source)) {
            return true;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() && CloudEnchantmentRegistry.get().grantsDamageImmunity(this.getArmor().getItem(slot.getArmorSlot()), slot, source)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected DoubleUnaryOperator createDamageReduction(DamageSource source) {
        DoubleUnaryOperator armor = source.getDamageType().is(DamageTypeTags.BYPASSES_ARMOR) ? damage -> damage : this.createArmorReduction(source);
        DoubleUnaryOperator effects = super.createDamageReduction(source);
        float enchantmentFactor = source.getDamageType().is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.getDamageType().is(DamageTypeTags.BYPASSES_ENCHANTMENTS) ? 1 : this.getEnchantmentDamageFactor(source);
        return damage -> Math.max(0, effects.applyAsDouble(armor.applyAsDouble(damage)) * enchantmentFactor);
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
        double defense = armorPoints;
        double minimumArmor = Math.min(20, defense * 0.2);
        return damage -> {
            double effectiveArmor = Math.clamp(defense - damage / toughnessFactor, minimumArmor, 20);
            float effectiveness = CloudEnchantmentRegistry.get().modifyArmorEffectiveness(source.getWeaponItem(), (float) (effectiveArmor / 25));
            return damage * (1 - effectiveness);
        };
    }

    private float getEnchantmentDamageFactor(DamageSource source) {
        float enchantmentProtection = 0;
        ArmorView armorView = this.getArmor();
        for (int armorSlot = 0; armorSlot < armorView.size(); armorSlot++) {
            enchantmentProtection += CloudEnchantmentRegistry.get().getDamageProtection(armorView.getItem(armorSlot), source);
        }

        float enchantmentReduction = Math.clamp(enchantmentProtection, 0, 20) * 0.04f;
        return 1 - enchantmentReduction;
    }

    @Override
    protected void applyDamageEffects(CloudEntityDamageEvent source, float damageBeforeReductions) {
        super.applyDamageEffects(source, damageBeforeReductions);
        if (source.getDamageType().is(DamageTypeTags.DAMAGES_HELMET)) {
            this.hurtHelmet(source.getHelmetDamage());
        }

        if (!source.getDamageType().is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(damageBeforeReductions);
        }

        Entity damager = source.getDamageSource().getCausingEntity();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack armor = this.getArmor().getItem(slot);
            ItemStack damagedArmor = armor;
            if (damager != null && !damagedArmor.isEmpty()) {
                damagedArmor = CloudEnchantmentRegistry.get().applyPostHurtEffects(damagedArmor, this, damager);
            }

            if (!damagedArmor.equals(armor) && this.getArmor().getItem(slot).equals(armor)) {
                this.getArmor().setItem(slot, damagedArmor);
            }
        }
    }

    protected void hurtHelmet(float damage) {
    }

    protected void hurtArmor(float damage) {
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
    protected @Nullable DeathProtectionUse findDeathProtection() {
        ItemStack item = this.getOffhand().getOffhandItem();
        DeathProtectionComponent protection = item.isEmpty() ? null : CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.DEATH_PROTECTION);
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
