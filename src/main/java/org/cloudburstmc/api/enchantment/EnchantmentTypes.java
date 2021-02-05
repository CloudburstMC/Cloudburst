package org.cloudburstmc.api.enchantment;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class EnchantmentTypes {

    public static final EnchantmentType PROTECTION = EnchantmentType.builder().id((short) 0).type(Identifiers.PROTECTION).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType FIRE_PROTECTION = EnchantmentType.builder().id((short) 1).type(Identifiers.FIRE_PROTECTION).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType FALL_PROTECTION = EnchantmentType.builder().id((short) 2).type(Identifiers.FALL_PROTECTION).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType EXPLOSION_PROTECTION = EnchantmentType.builder().id((short) 3).type(Identifiers.EXPLOSION_PROTECTION).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType PROJECTILE_PROTECTION = EnchantmentType.builder().id((short) 4).type(Identifiers.PROJECTILE_PROTECTION).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType THORNS = EnchantmentType.builder().id((short) 5).type(Identifiers.THORNS).maxLevel(3).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType WATER_BREATHING = EnchantmentType.builder().id((short) 6).type(Identifiers.RESPIRATION).maxLevel(3).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType WATER_WALKER = EnchantmentType.builder().id((short) 7).type(Identifiers.WATER_WALKER).maxLevel(3).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType WATER_WORKER = EnchantmentType.builder().id((short) 8).type(Identifiers.WATER_WORKER).maxLevel(1).weight(2).target(EnchantmentTarget.ARMOR_HEAD).build();
    public static final EnchantmentType DAMAGE_SHARPNESS = EnchantmentType.builder().id((short) 9).type(Identifiers.DAMAGE_SHARPNESS).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType DAMAGE_SMITE = EnchantmentType.builder().id((short) 10).type(Identifiers.DAMAGE_SMITE).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType DAMAGE_ARTHOPODS = EnchantmentType.builder().id((short) 11).type(Identifiers.DAMAGE_ARTHOPODS).maxLevel(5).weight(5).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType KNOCKBACK = EnchantmentType.builder().id((short) 12).type(Identifiers.KNOCKBACK).maxLevel(2).weight(5).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType FIRE_ASPECT = EnchantmentType.builder().id((short) 13).type(Identifiers.FIRE_ASPECT).maxLevel(2).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType LOOTING = EnchantmentType.builder().id((short) 14).type(Identifiers.LOOTING).maxLevel(3).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType EFFICIENCY = EnchantmentType.builder().id((short) 15).type(Identifiers.EFFICIENCY).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType SILK_TOUCH = EnchantmentType.builder().id((short) 16).type(Identifiers.SILK_TOUCH).maxLevel(1).weight(1).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType DURABILITY = EnchantmentType.builder().id((short) 17).type(Identifiers.DURABILITY).maxLevel(3).weight(5).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType FORTUNE = EnchantmentType.builder().id((short) 18).type(Identifiers.FORTUNE).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType BOW_POWER = EnchantmentType.builder().id((short) 19).type(Identifiers.BOW_POWER).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType BOW_PUNCH = EnchantmentType.builder().id((short) 20).type(Identifiers.BOW_PUNCH).maxLevel(2).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType BOW_FLAME = EnchantmentType.builder().id((short) 21).type(Identifiers.BOW_FLAME).maxLevel(1).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType BOW_INFINITY = EnchantmentType.builder().id((short) 22).type(Identifiers.BOW_INFINITY).maxLevel(1).weight(1).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType LUCK = EnchantmentType.builder().id((short) 23).type(Identifiers.LUCK).maxLevel(3).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType LURE = EnchantmentType.builder().id((short) 24).type(Identifiers.LURE).maxLevel(3).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType FROST_WALKER = EnchantmentType.builder().id((short) 25).type(Identifiers.FROST_WALKER).maxLevel(2).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType MENDING = EnchantmentType.builder().id((short) 26).type(Identifiers.MENDING).maxLevel(1).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType BINDING_CURSE = EnchantmentType.builder().id((short) 27).type(Identifiers.BINDING_CURSE).maxLevel(1).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType VANISHING_CURSE = EnchantmentType.builder().id((short) 28).type(Identifiers.VANISHING_CURSE).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType TRIDENT_IMPALING = EnchantmentType.builder().id((short) 29).type(Identifiers.TRIDENT_IMPALING).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType TRIDENT_RIPTIDE = EnchantmentType.builder().id((short) 30).type(Identifiers.TRIDENT_RIPTIDE).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType TRIDENT_LOYALTY = EnchantmentType.builder().id((short) 31).type(Identifiers.TRIDENT_LOYALTY).maxLevel(5).weight(5).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType TRIDENT_CHANNELING = EnchantmentType.builder().id((short) 32).type(Identifiers.TRIDENT_CHANNELING).maxLevel(5).weight(1).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType CROSSBOW_MULTISHOT = EnchantmentType.builder().id((short) 33).type(Identifiers.CROSSBOW_MULTISHOT).maxLevel(5).weight(2).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType CROSSBOW_PIERCING = EnchantmentType.builder().id((short) 34).type(Identifiers.CROSSBOW_PIERCING).maxLevel(5).weight(10).target(EnchantmentTarget.ARMOR).build();
    public static final EnchantmentType CROSSBOW_QUICK_CHARGE = EnchantmentType.builder().id((short) 35).type(Identifiers.CROSSBOW_QUICK_CHARGE).maxLevel(5).weight(5).target(EnchantmentTarget.ARMOR).build();
}
