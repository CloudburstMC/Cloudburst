package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MinecartDisplayTest {

    @Test
    void commandBlockMinecartDropsItsOwnVariant() {
        Location location = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));
        EntityCommandBlockMinecart cart = new EntityCommandBlockMinecart(EntityTypes.COMMAND_BLOCK_MINECART, location);

        assertEquals(ItemTypes.COMMAND_BLOCK_MINECART, cart.getDropItemType());
    }

    @Test
    void preservesDisplayOffsetAndRemovesSavedOverrideWhenReset() {
        EntityMinecart cart = new EntityMinecart(EntityTypes.MINECART, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        assertEquals(6, cart.getDisplayOffset());
        assertFalse(cart.hasDisplay());

        cart.loadAdditionalData(NbtMap.builder().putInt("DisplayOffset", 12).build());
        NbtMapBuilder saved = NbtMap.builder();
        cart.saveAdditionalData(saved);
        assertEquals(12, saved.build().getInt("DisplayOffset"));

        cart.setDisplayOffset(6);
        saved = NbtMap.builder();
        cart.saveAdditionalData(saved);
        assertFalse(saved.containsKey("DisplayOffset"));

        cart.setDisplayOffset(12);
        cart.loadAdditionalData(NbtMap.EMPTY);
        assertEquals(6, cart.getDisplayOffset());
    }
}
