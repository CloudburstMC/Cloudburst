package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.component.GetBlockStateHandler;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Set;

import static org.cloudburstmc.api.item.ItemTypes.*;

@UtilityClass
public class ShulkerBoxItems {

    public static final Set<ItemType> TYPES = Set.of(
            UNDYED_SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
            LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX,
            GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX,
            BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX, BLACK_SHULKER_BOX
    );

    public static boolean isBox(Identifier id) {
        return TYPES.stream().anyMatch(type -> type.getId().equals(id));
    }

    public static boolean accepts(ItemStack item) {
        return item.isEmpty() || !TYPES.contains(item.getType());
    }

    public static ItemStack recolor(ItemStack box, ItemType type) {
        GetBlockStateHandler placement = CloudItemRegistry.get().requireComponent(type, ItemBehaviors.GET_BLOCK);
        BlockState state = placement.execute(ItemStack.from(type)).orElseThrow();
        return box.toBuilder().itemType(type).amount(1).setData(ItemDataComponents.BLOCK_STATE, state)
                .removeData(ItemDataComponents.COLOR).build();
    }
}
