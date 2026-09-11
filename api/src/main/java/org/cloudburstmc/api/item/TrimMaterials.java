package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TrimMaterials {
    public static final TrimMaterial AMETHYST = new TrimMaterial("amethyst", MinecraftTextColor.MATERIAL_AMETHYST, ItemTypes.AMETHYST_SHARD.getId());
    public static final TrimMaterial COPPER = new TrimMaterial("copper", MinecraftTextColor.MATERIAL_COPPER, ItemTypes.COPPER_INGOT.getId());
    public static final TrimMaterial DIAMOND = new TrimMaterial("diamond", MinecraftTextColor.MATERIAL_DIAMOND, ItemTypes.DIAMOND.getId());
    public static final TrimMaterial EMERALD = new TrimMaterial("emerald", MinecraftTextColor.MATERIAL_EMERALD, ItemTypes.EMERALD.getId());
    public static final TrimMaterial GOLD = new TrimMaterial("gold", MinecraftTextColor.MATERIAL_GOLD, ItemTypes.GOLD_INGOT.getId());
    public static final TrimMaterial IRON = new TrimMaterial("iron", MinecraftTextColor.MATERIAL_IRON, ItemTypes.IRON_INGOT.getId());
    public static final TrimMaterial LAPIS = new TrimMaterial("lapis", MinecraftTextColor.MATERIAL_LAPIS, ItemTypes.LAPIS_LAZULI.getId());
    public static final TrimMaterial NETHERITE = new TrimMaterial("netherite", MinecraftTextColor.MATERIAL_NETHERITE, ItemTypes.NETHERITE_INGOT.getId());
    public static final TrimMaterial QUARTZ = new TrimMaterial("quartz", MinecraftTextColor.MATERIAL_QUARTZ, ItemTypes.QUARTZ.getId());
    public static final TrimMaterial REDSTONE = new TrimMaterial("redstone", MinecraftTextColor.MATERIAL_REDSTONE, ItemTypes.REDSTONE.getId());
    public static final TrimMaterial RESIN = new TrimMaterial("resin", MinecraftTextColor.MATERIAL_RESIN, ItemTypes.RESIN_BRICK.getId());
}
