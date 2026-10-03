package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
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
    void preservesDisplayOffsetAndRemovesSavedOverrideWhenReset() {
        EntityMinecart cart = new EntityMinecart(EntityTypes.MINECART, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        assertEquals(6, cart.getDisplayOffset());
        assertFalse(cart.hasDisplay());

        cart.loadAdditionalData(NbtMap.builder().putInt("DisplayOffset", 12).build());
        NbtMapBuilder saved = NbtMap.builder();
        cart.saveAdditionalData(saved);
        assertEquals(12, saved.build().getInt("DisplayOffset"));

        cart.setDisplayBlockOffset(6);
        saved = NbtMap.builder();
        cart.saveAdditionalData(saved);
        assertFalse(saved.containsKey("DisplayOffset"));

        cart.setDisplayBlockOffset(12);
        cart.loadAdditionalData(NbtMap.EMPTY);
        assertEquals(6, cart.getDisplayOffset());
    }
}
