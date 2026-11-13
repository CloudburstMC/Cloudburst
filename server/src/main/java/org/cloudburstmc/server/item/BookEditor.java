package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.player.PlayerEditBookEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BookPage;
import org.cloudburstmc.api.item.data.WrittenBook;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource;
import org.cloudburstmc.protocol.bedrock.packet.BookEditPacket;
import org.cloudburstmc.server.player.CloudPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class BookEditor {

    private static final int MAX_PAGES = 50;
    private static final int MAX_PAGE_LENGTH = 256;
    private static final int MAX_SIGNATURE_LENGTH = 16;

    public static boolean isInventoryPrediction(List<InventoryActionData> actions) {
        if (actions.size() != 3) {
            return false;
        }

        InventoryActionData slot = actions.getFirst();
        InventoryActionData oldBook = actions.get(1);
        InventoryActionData newBook = actions.get(2);
        if (slot.getSource().getType() != InventorySource.Type.CONTAINER
                || slot.getSource().getContainerId() != ContainerId.INVENTORY
                || slot.getSlot() < 0 || slot.getSlot() > 8
                || oldBook.getSource().getType() != InventorySource.Type.CREATIVE
                || newBook.getSource().getType() != InventorySource.Type.CREATIVE) {
            return false;
        }

        ItemType resultType = isType(slot.getToItem(), ItemTypes.WRITTEN_BOOK) ? ItemTypes.WRITTEN_BOOK : ItemTypes.WRITABLE_BOOK;
        return isType(slot.getFromItem(), ItemTypes.WRITABLE_BOOK)
                && isType(slot.getToItem(), resultType)
                && oldBook.getFromItem().getCount() == 0
                && isType(oldBook.getToItem(), ItemTypes.WRITABLE_BOOK)
                && isType(newBook.getFromItem(), resultType)
                && newBook.getToItem().getCount() == 0;
    }

    public static boolean edit(CloudPlayer player, BookEditPacket packet) {
        int slot = packet.getInventorySlot();
        if (slot != player.getSelectedHotbarSlot()) {
            return false;
        }

        ItemStack original = player.getInventory().getSelectedItem();
        if (original.getType() != ItemTypes.WRITABLE_BOOK || original.isEmpty() || packet.getAction() == null) {
            return false;
        }

        ItemStack edited = propose(player, packet, original);
        if (edited == null) {
            return false;
        }

        PlayerEditBookEvent event = new PlayerEditBookEvent(player, slot, original, edited, packet.getAction() == BookEditPacket.Action.SIGN_BOOK);
        player.getServer().getEventManager().fire(event);
        ItemStack replacement = event.getNewBook();
        if (event.isCancelled() || slot != player.getSelectedHotbarSlot()
                || !original.equals(player.getInventory().getSelectedItem())
                || replacement.isEmpty() || replacement.getType() != edited.getType()) {
            return false;
        }

        player.getInventory().setSelectedItem(replacement.withCount(original.getCount()));
        return true;
    }

    private static boolean isType(ItemData item, ItemType type) {
        return item.getCount() > 0 && item.getDefinition().getIdentifier().equals(type.getId().toString());
    }

    private static @Nullable ItemStack propose(CloudPlayer player, BookEditPacket packet, ItemStack original) {
        WrittenBook book = original.get(ItemDataComponents.BOOK_DATA);
        List<BookPage> pages = new ArrayList<>(book == null ? List.of() : book.getPages());
        if (packet.getAction() == BookEditPacket.Action.SIGN_BOOK) {
            String title = packet.getTitle();
            String author = packet.getAuthor();
            if (title == null || title.isBlank() || title.length() > MAX_SIGNATURE_LENGTH || author == null || author.length() > MAX_SIGNATURE_LENGTH || pages.size() > MAX_PAGES) {
                return null;
            }

            WrittenBook signed = WrittenBook.of(0, title, author, player.getXuid(), 0, true, pages);
            return original.toBuilder().itemType(ItemTypes.WRITTEN_BOOK).setData(ItemDataComponents.BOOK_DATA, signed).build();
        }

        int index = packet.getPageNumber();
        if (index < 0 || index >= MAX_PAGES) {
            return null;
        }

        switch (packet.getAction()) {
            case ADD_PAGE, REPLACE_PAGE -> {
                String text = packet.getText();
                if (text == null || text.length() > MAX_PAGE_LENGTH) {
                    return null;
                }

                BookPage page = new BookPage(text, packet.getPhotoName() == null ? "" : packet.getPhotoName());
                boolean insert = packet.getAction() == BookEditPacket.Action.ADD_PAGE;
                if (insert && pages.size() >= MAX_PAGES) {
                    return null;
                }

                while (pages.size() < index) {
                    pages.add(new BookPage("", ""));
                }

                if (insert || pages.size() == index) {
                    pages.add(index, page);
                } else {
                    pages.set(index, page);
                }
            }
            case DELETE_PAGE -> {
                if (index >= pages.size()) {
                    return null;
                }
                pages.remove(index);
            }
            case SWAP_PAGES -> {
                int other = packet.getSecondaryPageNumber();
                if (index >= pages.size() || other < 0 || other >= pages.size()) {
                    return null;
                }
                Collections.swap(pages, index, other);
            }
            case SIGN_BOOK -> throw new IllegalStateException("Signature must be handled before page edits");
        }

        WrittenBook draft = WrittenBook.of(0, "", "", "", 0, false, pages);
        return original.toBuilder().setData(ItemDataComponents.BOOK_DATA, draft).build();
    }
}
