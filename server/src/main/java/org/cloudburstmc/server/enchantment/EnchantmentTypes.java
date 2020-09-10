package org.cloudburstmc.server.enchantment;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.utils.Identifier;

@UtilityClass
public class EnchantmentTypes {

    //TODO: implement enchantment target
    public static final EnchantmentType PROTECTION = CloudEnchantmentType.builder().id(0).type(Identifier.fromString("protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FIRE_PROTECTION = CloudEnchantmentType.builder().id(1).type(Identifier.fromString("fire_protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FALL_PROTECTION = CloudEnchantmentType.builder().id(2).type(Identifier.fromString("feather_falling")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType EXPLOSION_PROTECTION = CloudEnchantmentType.builder().id(3).type(Identifier.fromString("blast_protection")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType PROJECTILE_PROTECTION = CloudEnchantmentType.builder().id(4).type(Identifier.fromString("projectile_protection")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType THORNS = CloudEnchantmentType.builder().id(5).type(Identifier.fromString("thorns")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_BREATHING = CloudEnchantmentType.builder().id(6).type(Identifier.fromString("respiration")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_WALKER = CloudEnchantmentType.builder().id(7).type(Identifier.fromString("depth_strider")).maxLevel(3).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType WATER_WORKER = CloudEnchantmentType.builder().id(8).type(Identifier.fromString("aqua_affinity")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR_HEAD)*/.build();
    public static final EnchantmentType SHARPNESS = CloudEnchantmentType.builder().id(9).type(Identifier.fromString("sharpness")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType SMITE = CloudEnchantmentType.builder().id(10).type(Identifier.fromString("smite")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType DAMAGE_ARTHOPODS = CloudEnchantmentType.builder().id(11).type(Identifier.fromString("bane_of_arthopods")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType KNOCKBACK = CloudEnchantmentType.builder().id(12).type(Identifier.fromString("knockback")).maxLevel(2).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FIRE_ASPECT = CloudEnchantmentType.builder().id(13).type(Identifier.fromString("fire_aspect")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LOOTING = CloudEnchantmentType.builder().id(14).type(Identifier.fromString("looting")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType EFFICIENCY = CloudEnchantmentType.builder().id(15).type(Identifier.fromString("efficiency")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType SILK_TOUCH = CloudEnchantmentType.builder().id(16).type(Identifier.fromString("silk_touch")).maxLevel(1).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType DURABILITY = CloudEnchantmentType.builder().id(17).type(Identifier.fromString("unbreaking")).maxLevel(3).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FORTUNE = CloudEnchantmentType.builder().id(18).type(Identifier.fromString("fortune")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType POWER = CloudEnchantmentType.builder().id(19).type(Identifier.fromString("power")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType PUNCH = CloudEnchantmentType.builder().id(20).type(Identifier.fromString("punch")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FLAME = CloudEnchantmentType.builder().id(21).type(Identifier.fromString("flame")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType INFINITY = CloudEnchantmentType.builder().id(22).type(Identifier.fromString("infinity")).maxLevel(1).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LUCK = CloudEnchantmentType.builder().id(23).type(Identifier.fromString("luck_of_the_sea")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LURE = CloudEnchantmentType.builder().id(24).type(Identifier.fromString("lure")).maxLevel(3).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType FROST_WALKER = CloudEnchantmentType.builder().id(25).type(Identifier.fromString("frost_walker")).maxLevel(2).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType MENDING = CloudEnchantmentType.builder().id(26).type(Identifier.fromString("mending")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType BINDING_CURSE = CloudEnchantmentType.builder().id(27).type(Identifier.fromString("binding")).maxLevel(1).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType VANISHING_CURSE = CloudEnchantmentType.builder().id(28).type(Identifier.fromString("vanishing")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType IMPALING = CloudEnchantmentType.builder().id(29).type(Identifier.fromString("impaling")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType RIPTIDE = CloudEnchantmentType.builder().id(30).type(Identifier.fromString("riptide")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType LOYALTY = CloudEnchantmentType.builder().id(31).type(Identifier.fromString("loyalty")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CHANNELING = CloudEnchantmentType.builder().id(32).type(Identifier.fromString("channeling")).maxLevel(5).weight(1)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_MULTISHOT = CloudEnchantmentType.builder().id(33).type(Identifier.fromString("multishot")).maxLevel(5).weight(2)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_PIERCING = CloudEnchantmentType.builder().id(34).type(Identifier.fromString("piercing")).maxLevel(5).weight(10)/*.target(EnchantmentTarget.ARMOR)*/.build();
    public static final EnchantmentType CROSSBOW_QUICK_CHARGE = CloudEnchantmentType.builder().id(35).type(Identifier.fromString("quick_charge")).maxLevel(5).weight(5)/*.target(EnchantmentTarget.ARMOR)*/.build();


    @RequiredArgsConstructor
    @Getter
    @Builder
    public class CloudEnchantmentType implements EnchantmentType {

        private final int id;
        private final Identifier type;
        private final int maxLevel;
        private final int weight;
        private final EnchantmentTarget target;

    }
}
