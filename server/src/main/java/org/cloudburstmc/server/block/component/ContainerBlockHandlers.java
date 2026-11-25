package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.server.container.screen.*;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class ContainerBlockHandlers {

    public static final UseBlockHandler ANVIL = open(CloudAnvilContainerScreen::new);
    public static final UseBlockHandler BARREL = open(CloudBlockStorageScreen::barrel);
    public static final UseBlockHandler BEACON = open(CloudBeaconContainerScreen::new);
    public static final UseBlockHandler BLAST_FURNACE = open(CloudFurnaceContainerScreen::blastFurnace);
    public static final UseBlockHandler BREWING_STAND = open(CloudBrewingContainerScreen::new);
    public static final UseBlockHandler CARTOGRAPHY_TABLE = open(CloudCartographyContainerScreen::new);
    public static final UseBlockHandler CHEST = openChest(CloudChestScreen::create);
    public static final UseBlockHandler CRAFTER = open(CloudCrafterContainerScreen::new);
    public static final UseBlockHandler CRAFTING_TABLE = open(CloudCraftingTableContainerScreen::new);
    public static final UseBlockHandler DISPENSER = open(CloudDispenserContainerScreen::new);
    public static final UseBlockHandler DROPPER = open(CloudDropperContainerScreen::new);
    public static final UseBlockHandler ENCHANTING_TABLE = open(CloudEnchantingContainerScreen::new);
    public static final UseBlockHandler ENDER_CHEST = openChest(CloudEnderChestScreen::create);
    public static final UseBlockHandler FURNACE = open(CloudFurnaceContainerScreen::furnace);
    public static final UseBlockHandler GRINDSTONE = open(CloudGrindstoneContainerScreen::new);
    public static final UseBlockHandler HOPPER = open(CloudHopperContainerScreen::new);
    public static final UseBlockHandler LOOM = open(CloudLoomContainerScreen::new);
    public static final UseBlockHandler SHULKER_BOX = open(CloudShulkerBoxContainerScreen::create);
    public static final UseBlockHandler SMITHING_TABLE = open(CloudSmithingContainerScreen::new);
    public static final UseBlockHandler SMOKER = open(CloudFurnaceContainerScreen::smoker);
    public static final UseBlockHandler STONECUTTER = open(CloudStonecutterContainerScreen::new);

    private static UseBlockHandler open(ScreenFactory factory) {
        return (block, player, direction, item) -> {
            if (!(player instanceof CloudPlayer cloudPlayer)) {
                return false;
            }
            return openScreen(cloudPlayer, block, factory);
        };
    }

    private static UseBlockHandler openChest(ScreenFactory factory) {
        return (block, player, _, _) -> {
            if (!(player instanceof CloudPlayer cloudPlayer)) {
                return false;
            }
            openScreen(cloudPlayer, block, factory);
            return true;
        };
    }

    private static boolean openScreen(CloudPlayer player, Block block, ScreenFactory factory) {
        if (player.hasOpenContainer()) {
            return false;
        }

        CloudInventoryScreen screen = factory.create(player, block);
        if (screen == null) {
            return false;
        }

        player.getInventoryManager().openScreen(screen);
        return true;
    }

    @FunctionalInterface
    private interface ScreenFactory {

        @Nullable
        CloudInventoryScreen create(CloudPlayer player, Block block);
    }
}
