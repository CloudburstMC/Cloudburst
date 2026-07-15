package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.blockentity.LecternBlockEntity;
import org.cloudburstmc.server.container.screen.CloudLecternContainerScreen;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class LecternBlockHandlers {

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        LecternBlockEntity lectern = CloudLecternContainerScreen.getOrCreateLectern(block);
        if (lectern == null) {
            return false;
        }

        if (lectern.hasBook()) {
            if (!cloudPlayer.canOpenInventory()) {
                return false;
            }
            cloudPlayer.getInventoryManager().openScreen(new CloudLecternContainerScreen(cloudPlayer, block));
            return true;
        }

        if (item.isEmpty() || !isLecternBook(item.getType())) {
            return false;
        }

        lectern.setBook(item);
        if (!cloudPlayer.isCreative()) {
            cloudPlayer.getInventory().setSelectedItem(item.withCount(item.getCount() - 1));
        }

        return true;
    };

    private static boolean isLecternBook(ItemType type) {
        return type == ItemTypes.WRITABLE_BOOK || type == ItemTypes.WRITTEN_BOOK;
    }
}
