package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;

/**
 * A {@link ContainerMapping} whose size and offset are derived from the UI absolute
 * slot layout for fixed UI-only slot types (anvil inputs, enchanting slots, cursor, etc.).
 *
 * <p><strong>Note:</strong> {@link ContainerSlotType#CREATED_OUTPUT} is intentionally
 * excluded here. The client sends {@code CREATED_OUTPUT} using an absolute slot
 * index of 50, so the normalization offset must be {@code -50} (not {@code +50}). That
 * mapping is handled directly in
 * {@link org.cloudburstmc.server.container.screen.CloudContainerScreen#setupMappings()}
 * using a plain {@link ContainerMapping}.</p>
 */
public class UIContainerMapping extends ContainerMapping {

    public UIContainerMapping(ContainerSlotType slotType, SlotGroup view) {
        super(slotType, view, getContainerSize(slotType), getContainerOffset(slotType));
    }

    private static int getContainerSize(ContainerSlotType type) {
        return switch (type) {
            case ANVIL_INPUT, ANVIL_MATERIAL, SMITHING_TABLE_INPUT, SMITHING_TABLE_MATERIAL, BEACON_PAYMENT,
                 ENCHANTING_INPUT, ENCHANTING_MATERIAL, TRADE_INGREDIENT_1, TRADE_INGREDIENT_2,
                 MATERIAL_REDUCER_INPUT, LOOM_INPUT, LOOM_DYE, LOOM_MATERIAL, TRADE2_INGREDIENT_1,
                 TRADE2_INGREDIENT_2, GRINDSTONE_INPUT, GRINDSTONE_ADDITIONAL, STONECUTTER_INPUT, CARTOGRAPHY_INPUT,
                 CARTOGRAPHY_ADDITIONAL, CURSOR -> 1;
            case COMPOUND_CREATOR_INPUT, MATERIAL_REDUCER_OUTPUT -> 9;
            default -> throw new UnsupportedOperationException("Unknown UI container slot type " + type);
        };
    }

    private static int getContainerOffset(ContainerSlotType type) {
        return switch (type) {
            case ANVIL_INPUT -> -1;
            case ANVIL_MATERIAL -> -2;
            case SMITHING_TABLE_INPUT -> -51;
            case SMITHING_TABLE_MATERIAL -> -52;
            case BEACON_PAYMENT -> -27;
            case ENCHANTING_INPUT -> -14;
            case ENCHANTING_MATERIAL -> -15;
            case TRADE_INGREDIENT_1 -> -6;
            case TRADE_INGREDIENT_2 -> -7;
            case COMPOUND_CREATOR_INPUT -> -18;
            case MATERIAL_REDUCER_INPUT -> -8;
            case MATERIAL_REDUCER_OUTPUT -> -41;
            case LOOM_INPUT -> -9;
            case LOOM_DYE -> -10;
            case LOOM_MATERIAL -> -11;
            case TRADE2_INGREDIENT_1 -> -4;
            case TRADE2_INGREDIENT_2 -> -5;
            case GRINDSTONE_INPUT -> -16;
            case GRINDSTONE_ADDITIONAL -> -17;
            case STONECUTTER_INPUT -> -3;
            case CARTOGRAPHY_INPUT -> -12;
            case CARTOGRAPHY_ADDITIONAL -> -13;
            case CURSOR -> 0;
            default -> throw new UnsupportedOperationException("Unknown UI container slot type " + type);
        };
    }
}
