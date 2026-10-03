package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChestBoatStorageTest {

    @Test
    void savesHullAndStorageWithoutDependingOnAnOpenScreen() {
        EntityChestBoat boat = boat();
        boat.setBoatType(BoatType.CHERRY);
        boat.getInventory().setItem(26, ItemStack.builder(ItemTypes.DIRT).amount(32).build());

        NbtMapBuilder saved = NbtMap.builder();
        boat.saveAdditionalData(saved);
        NbtMap data = saved.build();
        EntityChestBoat restored = boat();
        restored.loadAdditionalData(NbtMap.builder()
                .putInt("WoodType", data.getInt("WoodType"))
                .putList("Items", NbtType.COMPOUND, data.getList("Items", NbtType.COMPOUND))
                .build());

        assertEquals(BoatType.CHERRY, restored.getBoatType());
        assertEquals(boat.getInventory().getItem(26), restored.getInventory().getItem(26));
        assertTrue(restored.getInventory().getItem(0).isEmpty());
    }

    @Test
    void rejectsStorageSlotsOutsideTheBoatInventory() {
        NbtMap invalid = NbtMap.builder().putByte("Slot", (byte) 27).build();
        assertThrows(IllegalArgumentException.class, () -> boat().loadAdditionalData(NbtMap.builder()
                .putList("Items", NbtType.COMPOUND, invalid).build()));
    }

    private static EntityChestBoat boat() {
        return new EntityChestBoat(EntityTypes.CHEST_BOAT,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }
}
