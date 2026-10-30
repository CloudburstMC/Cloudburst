package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.ChestBoatData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the chest-boat branch. The stub fixture cannot build boats
 * ({@code initEntity} needs a live location), so the tests construct them directly
 * without initializing and drive {@code applyData}/{@code fillData}, asserting the
 * mapped NBT types exactly as the production save path would write them.
 */
class ChestBoatPersistenceTest {

    private static EntityChestBoat boat() {
        PersistenceFixtures.ensureInitialized();
        return new EntityChestBoat(EntityTypes.CHEST_BOAT,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        EntityChestBoat boat = boat();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack dirt = ItemStack.from(ItemTypes.DIRT, 1);

        ChestBoatData loaded = new ChestBoatData();
        loaded.setItems(List.of(new SlottedItem(0, diamond), new SlottedItem(5, dirt)));
        boat.applyData(loaded);

        assertEquals(diamond, boat.getInventory().getItem(0));
        assertEquals(dirt, boat.getInventory().getItem(5));

        ChestBoatData filled = new ChestBoatData();
        boat.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        List<NbtMap> expected = List.of(
                ItemUtils.serializeItem(diamond, 0),
                ItemUtils.serializeItem(dirt, 5));
        assertEquals(expected, written.getList("Items", NbtType.COMPOUND));
    }

    @Test
    void defaultInstanceWritesEmptyItems() {
        EntityChestBoat boat = boat();

        ChestBoatData filled = new ChestBoatData();
        boat.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save always writes the item list, empty for a fresh boat.
        assertEquals(List.of(), written.getList("Items", NbtType.COMPOUND));
        assertEquals(NbtType.COMPOUND, ((NbtList<?>) written.get("Items")).getType(),
                "empty Items must keep today's COMPOUND element type");
    }

    @Test
    void absentItemsClearTheInventory() {
        EntityChestBoat boat = boat();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ChestBoatData loaded = new ChestBoatData();
        loaded.setItems(List.of(new SlottedItem(0, diamond)));
        boat.applyData(loaded);

        // The inventory is always cleared before the stored items are read,
        // exactly as today's load does.
        boat.applyData(new ChestBoatData());

        assertTrue(boat.getInventory().getItem(0).isEmpty());

        ChestBoatData filled = new ChestBoatData();
        boat.fillData(filled);
        assertEquals(List.of(), ServerNbt.MAPPER.write(filled).getList("Items", NbtType.COMPOUND));
    }
}
