package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TrimMaterials {
    public static final TrimMaterial AMETHYST = new TrimMaterial("amethyst", BedrockNamedColor.MATERIAL_AMETHYST, ItemTypes.AMETHYST_SHARD.getId());
    public static final TrimMaterial COPPER = new TrimMaterial("copper", BedrockNamedColor.MATERIAL_COPPER, ItemTypes.COPPER_INGOT.getId());
    public static final TrimMaterial DIAMOND = new TrimMaterial("diamond", BedrockNamedColor.MATERIAL_DIAMOND, ItemTypes.DIAMOND.getId());
    public static final TrimMaterial EMERALD = new TrimMaterial("emerald", BedrockNamedColor.MATERIAL_EMERALD, ItemTypes.EMERALD.getId());
    public static final TrimMaterial GOLD = new TrimMaterial("gold", BedrockNamedColor.MATERIAL_GOLD, ItemTypes.GOLD_INGOT.getId());
    public static final TrimMaterial IRON = new TrimMaterial("iron", BedrockNamedColor.MATERIAL_IRON, ItemTypes.IRON_INGOT.getId());
    public static final TrimMaterial LAPIS = new TrimMaterial("lapis", BedrockNamedColor.MATERIAL_LAPIS, ItemTypes.LAPIS_LAZULI.getId());
    public static final TrimMaterial NETHERITE = new TrimMaterial("netherite", BedrockNamedColor.MATERIAL_NETHERITE, ItemTypes.NETHERITE_INGOT.getId());
    public static final TrimMaterial QUARTZ = new TrimMaterial("quartz", BedrockNamedColor.MATERIAL_QUARTZ, ItemTypes.QUARTZ.getId());
    public static final TrimMaterial REDSTONE = new TrimMaterial("redstone", BedrockNamedColor.MATERIAL_REDSTONE, ItemTypes.REDSTONE.getId());
    public static final TrimMaterial RESIN = new TrimMaterial("resin", BedrockNamedColor.MATERIAL_RESIN, ItemTypes.RESIN_BRICK.getId());
}
