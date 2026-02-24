package org.cloudburstmc.server.block.behavior;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.behavior.BooleanBlockBehavior;
import org.cloudburstmc.api.block.behavior.UseBlockBehavior;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.blockentity.LecternBlockEntity;
import org.cloudburstmc.server.container.screen.*;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.function.BiFunction;

@UtilityClass
public class ContainerBlockBehaviors {

    public static final BooleanBlockBehavior CAN_BE_USED = (behavior, block) -> true;

    public static final UseBlockBehavior ANVIL = open(CloudAnvilContainerScreen::new);
    public static final UseBlockBehavior BARREL = open(CloudChestContainerScreen::barrel);
    public static final UseBlockBehavior BEACON = open(CloudBeaconContainerScreen::new);
    public static final UseBlockBehavior BLAST_FURNACE = open(CloudFurnaceContainerScreen::blastFurnace);
    public static final UseBlockBehavior BREWING_STAND = open(CloudBrewingContainerScreen::new);
    public static final UseBlockBehavior CARTOGRAPHY_TABLE = open(CloudCartographyContainerScreen::new);
    public static final UseBlockBehavior CHEST = open(CloudChestContainerScreen::chest);
    public static final UseBlockBehavior CRAFTER = open(CloudCrafterContainerScreen::new);
    public static final UseBlockBehavior CRAFTING_TABLE = open(CloudCraftingTableContainerScreen::new);
    public static final UseBlockBehavior DISPENSER = open(CloudDispenserContainerScreen::new);
    public static final UseBlockBehavior DROPPER = open(CloudDropperContainerScreen::new);
    public static final UseBlockBehavior ENCHANTING_TABLE = open(CloudEnchantingContainerScreen::new);
    public static final UseBlockBehavior ENDER_CHEST = open(CloudEnderChestScreen::new);
    public static final UseBlockBehavior FURNACE = open(CloudFurnaceContainerScreen::furnace);
    public static final UseBlockBehavior GRINDSTONE = open(CloudGrindstoneContainerScreen::new);
    public static final UseBlockBehavior HOPPER = open(CloudHopperContainerScreen::new);
    public static final UseBlockBehavior LOOM = open(CloudLoomContainerScreen::new);
    public static final UseBlockBehavior SHULKER_BOX = open(CloudChestContainerScreen::shulkerBox);
    public static final UseBlockBehavior SMITHING_TABLE = open(CloudSmithingContainerScreen::new);
    public static final UseBlockBehavior SMOKER = open(CloudFurnaceContainerScreen::smoker);
    public static final UseBlockBehavior STONECUTTER = open(CloudStonecutterContainerScreen::new);
    public static final UseBlockBehavior TRAPPED_CHEST = open(CloudChestContainerScreen::chest);

    public static final UseBlockBehavior LECTERN = (behavior, block, player, direction) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        LecternBlockEntity lectern = CloudLecternContainerScreen.getOrCreateLectern(block);
        if (lectern == null) {
            return false;
        }

        if (!lectern.hasBook()) {
            ItemStack held = cloudPlayer.getInventory().getSelectedItem();
            if (held == null || held == ItemStack.EMPTY || held.getType() != ItemTypes.WRITABLE_BOOK) {
                return false;
            }
            lectern.setBook(held);
            if (!cloudPlayer.isCreative()) {
                cloudPlayer.getInventory().setSelectedItem(held.withCount(held.getCount() - 1));
            }
            return true;
        }

        return openScreen(cloudPlayer, block, CloudLecternContainerScreen::new);
    };

    private static UseBlockBehavior open(BiFunction<CloudPlayer, Block, ? extends CloudInventoryScreen> factory) {
        return (behavior, block, player, direction) -> {
            if (!(player instanceof CloudPlayer cloudPlayer)) {
                return false;
            }
            return openScreen(cloudPlayer, block, factory);
        };
    }

    private static boolean openScreen(CloudPlayer player, Block block, BiFunction<CloudPlayer, Block, ? extends CloudInventoryScreen> factory) {
        if (!player.canOpenInventory()) {
            return false;
        }
        player.getInventoryManager().openScreen(factory.apply(player, block));
        return true;
    }
}
