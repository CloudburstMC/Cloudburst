package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreativeItemRegistrationTest {

    @BeforeAll
    static void registerBlocks() {
        assertFalse(BlockTestRegistry.get().values().isEmpty());
    }

    @Test
    void appendedEntriesPreserveStackDataAndUseDistinctNetworkIds() {
        CloudItemRegistry registry = CloudItemRegistry.get();
        ItemPalette palette = new ItemPalette();
        palette.registerVanillaCreativeItems();
        List<CreativeItemData> vanilla = palette.getCreativeContentPacket().getContents();
        assertFalse(vanilla.isEmpty());

        ItemStack arrow = ItemStack.from(ItemTypes.ARROW).toBuilder()
                .amount(12)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON)
                .build();
        palette.addCreativeItem(arrow);

        List<CreativeItemData> entries = palette.getCreativeContentPacket().getContents();
        CreativeItemData appended = entries.getLast();

        assertEquals(vanilla.size() + 1, entries.size());
        assertEquals(entries.size(), entries.stream().map(CreativeItemData::getNetId).distinct().count());
        assertEquals(arrow, ItemUtils.fromNetwork(appended.getItem()));
        assertTrue(palette.isCreativeItem(registry.getId(ItemTypes.ARROW)));

        CreativeContentPacket packet = palette.getCreativeContentPacket();
        CreativeItemGroup group = packet.getGroups().get(appended.getGroupId());
        assertTrue(group.getName().isEmpty());
        assertSame(appended, palette.getCreativeItem(appended.getNetId()));
        assertNull(palette.getCreativeItem(0));
        assertNull(palette.getCreativeItem(-1));
        assertNull(palette.getCreativeItem(Integer.MAX_VALUE));

        palette.registerVanillaCreativeItems();
        assertEquals(entries.size(), palette.getCreativeContentPacket().getContents().size());
    }

    @Test
    void appendingInvalidatesTheCreativeContentSnapshot() {
        ItemPalette palette = new ItemPalette();
        CreativeContentPacket before = palette.getCreativeContentPacket();

        palette.addCreativeItem(ItemStack.from(ItemTypes.APPLE));

        CreativeContentPacket after = palette.getCreativeContentPacket();
        assertNotSame(before, after);
        assertTrue(before.getContents().isEmpty());
        assertEquals(1, after.getContents().size());

        after.getContents().clear();
        after.getGroups().clear();
        assertEquals(1, palette.getCreativeContentPacket().getContents().size());
        assertEquals(1, palette.getCreativeContentPacket().getGroups().size());
    }
}
