package org.cloudburstmc.server.enchantment;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.utils.Identifier;

@UtilityClass
public class EnchantmentTypes {

    //TODO: implement enchantment target
    public static final EnchantmentType PROTECTION = CloudEnchantmentType.builder().id((short) 0).type(Identifier.fromString("protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FIRE_PROTECTION = CloudEnchantmentType.builder().id((short) 1).type(Identifier.fromString("fire_protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FALL_PROTECTION = CloudEnchantmentType.builder().id((short) 2).type(Identifier.fromString("feather_falling")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType EXPLOSION_PROTECTION = CloudEnchantmentType.builder().id((short) 3).type(Identifier.fromString("blast_protection")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType PROJECTILE_PROTECTION = CloudEnchantmentType.builder().id((short) 4).type(Identifier.fromString("projectile_protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType THORNS = CloudEnchantmentType.builder().id((short) 5).type(Identifier.fromString("thorns")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_BREATHING = CloudEnchantmentType.builder().id((short) 6).type(Identifier.fromString("respiration")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_WALKER = CloudEnchantmentType.builder().id((short) 7).type(Identifier.fromString("depth_strider")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_WORKER = CloudEnchantmentType.builder().id((short) 8).type(Identifier.fromString("aqua_affinity")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR_HEAD)*/.build();
    public static final EnchantmentType DAMAGE_SHARPNESS = CloudEnchantmentType.builder().id((short) 9).type(Identifier.fromString("sharpness")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType DAMAGE_SMITE = CloudEnchantmentType.builder().id((short) 10).type(Identifier.fromString("smite")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType DAMAGE_ARTHOPODS = CloudEnchantmentType.builder().id((short) 11).type(Identifier.fromString("bane_of_arthopods")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType KNOCKBACK = CloudEnchantmentType.builder().id((short) 12).type(Identifier.fromString("knockback")).maxLevel(2).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FIRE_ASPECT = CloudEnchantmentType.builder().id((short) 13).type(Identifier.fromString("fire_aspect")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LOOTING = CloudEnchantmentType.builder().id((short) 14).type(Identifier.fromString("looting")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType EFFICIENCY = CloudEnchantmentType.builder().id((short) 15).type(Identifier.fromString("efficiency")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType SILK_TOUCH = CloudEnchantmentType.builder().id((short) 16).type(Identifier.fromString("silk_touch")).maxLevel(1).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType DURABILITY = CloudEnchantmentType.builder().id((short) 17).type(Identifier.fromString("unbreaking")).maxLevel(3).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FORTUNE = CloudEnchantmentType.builder().id((short) 18).type(Identifier.fromString("fortune")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BOW_POWER = CloudEnchantmentType.builder().id((short) 19).type(Identifier.fromString("power")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BOW_PUNCH = CloudEnchantmentType.builder().id((short) 20).type(Identifier.fromString("punch")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BOW_FLAME = CloudEnchantmentType.builder().id((short) 21).type(Identifier.fromString("flame")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BOW_INFINITY = CloudEnchantmentType.builder().id((short) 22).type(Identifier.fromString("infinity")).maxLevel(1).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LUCK = CloudEnchantmentType.builder().id((short) 23).type(Identifier.fromString("luck_of_the_sea")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LURE = CloudEnchantmentType.builder().id((short) 24).type(Identifier.fromString("lure")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FROST_WALKER = CloudEnchantmentType.builder().id((short) 25).type(Identifier.fromString("frost_walker")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType MENDING = CloudEnchantmentType.builder().id((short) 26).type(Identifier.fromString("mending")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BINDING_CURSE = CloudEnchantmentType.builder().id((short) 27).type(Identifier.fromString("binding")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType VANISHING_CURSE = CloudEnchantmentType.builder().id((short) 28).type(Identifier.fromString("vanishing")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType TRIDENT_IMPALING = CloudEnchantmentType.builder().id((short) 29).type(Identifier.fromString("impaling")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType TRIDENT_RIPTIDE = CloudEnchantmentType.builder().id((short) 30).type(Identifier.fromString("riptide")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType TRIDENT_LOYALTY = CloudEnchantmentType.builder().id((short) 31).type(Identifier.fromString("loyalty")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType TRIDENT_CHANNELING = CloudEnchantmentType.builder().id((short) 32).type(Identifier.fromString("channeling")).maxLevel(5).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_MULTISHOT = CloudEnchantmentType.builder().id((short) 33).type(Identifier.fromString("multishot")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_PIERCING = CloudEnchantmentType.builder().id((short) 34).type(Identifier.fromString("piercing")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_QUICK_CHARGE = CloudEnchantmentType.builder().id((short) 35).type(Identifier.fromString("quick_charge")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();


    @RequiredArgsConstructor
    @Getter
    @Builder
    public static class CloudEnchantmentType implements EnchantmentType {

        private final short id;
        private final Identifier type;
        private final int maxLevel;
        private final int weight;
        private final EnchantmentTarget target;

    }
}
