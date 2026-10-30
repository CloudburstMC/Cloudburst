package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.AbstractMinecartData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the abstract-minecart branch. {@link EntityAbstractMinecart} is
 * abstract and has no registered type mapping to {@link AbstractMinecartData}, so the
 * tests drive {@code applyData}/{@code fillData} directly through a minimal subclass
 * and assert the mapped NBT types, exactly as the production save path would write
 * them. Numeric display data is still rejected outside the data path.
 */
class AbstractMinecartPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static final class TestMinecart extends EntityAbstractMinecart {
        TestMinecart(EntityType<?> type, Location location) {
            super(type, location);
        }

        @Override
        public MinecartType getMinecartType() {
            return MinecartType.valueOf(0);
        }

        @Override
        public boolean isRideable() {
            return true;
        }
    }

    private static TestMinecart minecart() {
        PersistenceFixtures.ensureInitialized();
        return new TestMinecart(EntityTypes.MINECART,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestMinecart minecart = minecart();

        AbstractMinecartData loaded = new AbstractMinecartData();
        loaded.setDisplayOffset(9);
        loaded.setCustomDisplayTile(true);
        loaded.setDisplayBlock(BlockStates.SAND);
        minecart.applyData(loaded);

        assertTrue(minecart.hasDisplay());
        assertEquals(BlockStates.SAND, minecart.getDisplayBlock());
        assertEquals(9, minecart.getDisplayOffset());

        AbstractMinecartData filled = new AbstractMinecartData();
        minecart.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("DisplayOffset", "CustomDisplayTile", "DisplayBlock"));
        assertEquals(expectedKeys, written.keySet());
        assertTrue(written.get("DisplayOffset") instanceof Integer, "DisplayOffset must be an int");
        assertEquals(9, written.getInt("DisplayOffset"));
        assertEquals(NbtType.BYTE, NbtType.byClass(written.get("CustomDisplayTile").getClass()),
                "CustomDisplayTile must be a byte");
        assertTrue(written.getBoolean("CustomDisplayTile"));
        assertEquals(BlockStateCodec.write(BlockStates.SAND), written.getCompound("DisplayBlock"));
    }

    @Test
    void defaultInstanceWritesNoDisplayKeys() {
        TestMinecart minecart = minecart();

        AbstractMinecartData filled = new AbstractMinecartData();
        minecart.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save omits every display key for a plain cart at the default offset.
        assertEquals(BASE_KEYS, written.keySet());
        assertFalse(minecart.hasDisplay());
        assertEquals(6, minecart.getDisplayOffset());
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        TestMinecart minecart = minecart();
        minecart.setDisplay(true);
        minecart.setDisplayBlock(BlockStates.SAND);
        minecart.setDisplayBlockOffset(9);

        // All DTO fields null, as the mapper leaves them when the keys are absent.
        minecart.applyData(new AbstractMinecartData());

        assertTrue(minecart.hasDisplay());
        assertEquals(BlockStates.SAND, minecart.getDisplayBlock());
        assertEquals(6, minecart.getDisplayOffset());
    }

    @Test
    void numericDisplayDataStillRejected() {
        TestMinecart minecart = minecart();
        NbtMapBuilder tag = NbtMap.builder().putInt("DisplayTile", 1);

        CloudEntity entity = minecart;
        assertThrows(UnsupportedOperationException.class, () -> entity.loadAdditionalData(tag.build()));
    }
}
