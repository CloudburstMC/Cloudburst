package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemType;

import static org.cloudburstmc.api.item.ItemTypes.*;

@UtilityClass
public class VanillaCompostingData {

    public static void configure(CloudItemRegistry registry) {
        configure(registry, 30,
                ACACIA_LEAVES, ACACIA_SAPLING, AZALEA_LEAVES, BEETROOT_SEEDS, BIRCH_LEAVES,
                BIRCH_SAPLING, BUSH, CACTUS_FLOWER, CHERRY_LEAVES, CHERRY_SAPLING,
                DARK_OAK_LEAVES, DARK_OAK_SAPLING, DRIED_KELP, FIREFLY_BUSH, GLOW_BERRIES,
                GRASS_BLOCK, HANGING_ROOTS, JUNGLE_LEAVES, JUNGLE_SAPLING, KELP,
                LEAF_LITTER, MANGROVE_LEAVES, MANGROVE_PROPAGULE, MANGROVE_ROOTS, MELON_SEEDS,
                MOSS_CARPET, OAK_LEAVES, OAK_SAPLING, ORANGE_POPLAR_LEAVES, PALE_HANGING_MOSS,
                PALE_MOSS_CARPET, PALE_OAK_LEAVES, PALE_OAK_SAPLING, PINK_PETALS, PITCHER_POD,
                POPLAR_SAPLING, PUMPKIN_SEEDS, RED_POPLAR_LEAVES, RED_SHRUB, SEAGRASS,
                SHORT_DRY_GRASS, SHORT_GRASS, SMALL_DRIPLEAF_BLOCK, SPRUCE_LEAVES, SPRUCE_SAPLING,
                SWEET_BERRIES, TALL_DRY_GRASS, TORCHFLOWER_SEEDS, WHEAT_SEEDS, WILDFLOWERS,
                YELLOW_POPLAR_LEAVES
        );
        configure(registry, 50,
                AZALEA_LEAVES_FLOWERED, CACTUS, DRIED_KELP_BLOCK, GLOW_LICHEN, MELON_SLICE,
                NETHER_SPROUTS, SUGAR_CANE, TALL_GRASS, TWISTING_VINES, VINE,
                WEEPING_VINES
        );
        configure(registry, 65,
                ALLIUM, APPLE, AZALEA, AZURE_BLUET, BEETROOT,
                BIG_DRIPLEAF, BLUE_ORCHID, BROWN_MUSHROOM, CARROT, CARVED_PUMPKIN,
                CLOSED_EYEBLOSSOM, COCOA_BEANS, CORNFLOWER, CRIMSON_FUNGUS, CRIMSON_ROOTS,
                DANDELION, FERN, LARGE_FERN, LILAC, LILY_OF_THE_VALLEY,
                MELON_BLOCK, MOSS_BLOCK, MUSHROOM_STEM, NETHER_WART, OPEN_EYEBLOSSOM,
                ORANGE_TULIP, OXEYE_DAISY, PALE_MOSS_BLOCK, PEONY, PINK_TULIP,
                POPPY, POTATO, PUMPKIN, RED_MUSHROOM, RED_TULIP,
                ROSE_BUSH, SEA_PICKLE, SHELF_MUSHROOM, SHROOMLIGHT, SPORE_BLOSSOM,
                SUNFLOWER, WARPED_FUNGUS, WARPED_ROOTS, WATERLILY, WHEAT,
                WHITE_TULIP, WITHER_ROSE
        );
        configure(registry, 85,
                BAKED_POTATO, BREAD, BROWN_MUSHROOM_BLOCK, COOKIE, FLOWERING_AZALEA,
                HAY_BLOCK, NETHER_WART_BLOCK, PITCHER_PLANT, RED_MUSHROOM_BLOCK, STRAW_BED,
                TORCHFLOWER, WARPED_WART_BLOCK
        );
        configure(registry, 100,
                CAKE, PUMPKIN_PIE
        );
    }

    private static void configure(CloudItemRegistry registry, int chance, ItemType... items) {
        for (ItemType item : items) {
            registry.configure(item).set(ItemBehaviors.COMPOSTING_CHANCE, chance);
        }
    }
}
