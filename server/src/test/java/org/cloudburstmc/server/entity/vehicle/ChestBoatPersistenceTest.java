package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.ChestBoatData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the chest-boat branch. The stub fixture cannot build boats
 * ({@code initEntity} needs a live location), so the tests construct them directly
 * without initializing and drive the production load and save paths, asserting the
 * mapped NBT types.
 */
class ChestBoatPersistenceTest {

    private static EntityChestBoat boat() {
        PersistenceFixtures.ensureInitialized();
        return new EntityChestBoat(EntityTypes.CHEST_BOAT,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        EntityChestBoat boat = boat();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        ItemStack dirt = ItemStack.from(ItemTypes.DIRT, 1);

        boat.getInventory().setItem(0, diamond);
        boat.getInventory().setItem(5, dirt);

        assertEquals(diamond, boat.getInventory().getItem(0));
        assertEquals(dirt, boat.getInventory().getItem(5));

        NbtMap written = save(boat);

        List<NbtMap> expected = List.of(
                ItemUtils.serializeItem(diamond, 0),
                ItemUtils.serializeItem(dirt, 5));
        assertEquals(expected, written.getList("Items", NbtType.COMPOUND));
    }

    @Test
    void defaultInstanceWritesEmptyItems() {
        EntityChestBoat boat = boat();

        NbtMap written = save(boat);

        // Today's save always writes the item list, empty for a fresh boat.
        assertEquals(List.of(), written.getList("Items", NbtType.COMPOUND));
        assertEquals(NbtType.COMPOUND, ((NbtList<?>) written.get("Items")).getType(),
                "empty Items must keep today's COMPOUND element type");
    }

    @Test
    void absentItemsClearTheInventory() {
        EntityChestBoat boat = boat();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        boat.getInventory().setItem(0, diamond);

        // The inventory is always cleared before the stored items are read,
        // exactly as today's load does.
        boat.loadAdditionalData(NbtMap.EMPTY);

        assertTrue(boat.getInventory().getItem(0).isEmpty());

        assertEquals(List.of(), save(boat).getList("Items", NbtType.COMPOUND));
    }

    @Test
    void chestBoatKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityChestBoat.class, ChestBoatData.class,
                Map.of("inventory", "a view over the live container in data().getItems()"));
        Set<String> fields = new HashSet<>();
        for (Field field : EntityChestBoat.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("container")) {
            assertFalse(fields.contains(removed),
                    "EntityChestBoat keeps " + removed + "; state must live in ChestBoatData only");
        }
    }
}
