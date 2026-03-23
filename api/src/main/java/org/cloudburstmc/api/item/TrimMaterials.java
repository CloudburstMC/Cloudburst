package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TrimMaterials {
    public static final TrimMaterial AMETHYST = new TrimMaterial("amethyst", BedrockNamedColor.MATERIAL_AMETHYST, ItemIds.AMETHYST_SHARD);
    public static final TrimMaterial COPPER = new TrimMaterial("copper", BedrockNamedColor.MATERIAL_COPPER, ItemIds.COPPER_INGOT);
    public static final TrimMaterial DIAMOND = new TrimMaterial("diamond", BedrockNamedColor.MATERIAL_DIAMOND, ItemIds.DIAMOND);
    public static final TrimMaterial EMERALD = new TrimMaterial("emerald", BedrockNamedColor.MATERIAL_EMERALD, ItemIds.EMERALD);
    public static final TrimMaterial GOLD = new TrimMaterial("gold", BedrockNamedColor.MATERIAL_GOLD, ItemIds.GOLD_INGOT);
    public static final TrimMaterial IRON = new TrimMaterial("iron", BedrockNamedColor.MATERIAL_IRON, ItemIds.IRON_INGOT);
    public static final TrimMaterial LAPIS = new TrimMaterial("lapis", BedrockNamedColor.MATERIAL_LAPIS, ItemIds.LAPIS_LAZULI);
    public static final TrimMaterial NETHERITE = new TrimMaterial("netherite", BedrockNamedColor.MATERIAL_NETHERITE, ItemIds.NETHERITE_INGOT);
    public static final TrimMaterial QUARTZ = new TrimMaterial("quartz", BedrockNamedColor.MATERIAL_QUARTZ, ItemIds.QUARTZ);
    public static final TrimMaterial REDSTONE = new TrimMaterial("redstone", BedrockNamedColor.MATERIAL_REDSTONE, ItemIds.REDSTONE);
    public static final TrimMaterial RESIN = new TrimMaterial("resin", BedrockNamedColor.MATERIAL_RESIN, ItemIds.RESIN_BRICK);
}
