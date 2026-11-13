package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.MinecartType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudAbstractMinecartData;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestMinecart minecart = minecart();

        minecart.loadAdditionalData(NbtMap.builder()
                .putInt("DisplayOffset", 9)
                .putBoolean("CustomDisplayTile", true)
                .putCompound("DisplayBlock", BlockStateCodec.write(BlockStates.SAND))
                .build());

        assertTrue(minecart.hasDisplay());
        assertEquals(BlockStates.SAND, minecart.getDisplayBlock());
        assertEquals(9, minecart.getDisplayOffset());

        NbtMap written = save(minecart);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("DisplayOffset", "CustomDisplayTile", "DisplayBlock", "identifier"));
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

        NbtMap written = save(minecart);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, written.keySet());
        assertFalse(minecart.hasDisplay());
        assertEquals(6, minecart.getDisplayOffset());
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        TestMinecart minecart = minecart();
        minecart.setDisplay(true);
        minecart.setDisplayBlock(BlockStates.SAND);
        minecart.setDisplayBlockOffset(9);

        minecart.loadAdditionalData(NbtMap.EMPTY);

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

    @Test
    void legacyNumericDisplayBlockStaysOnSkipPath() {
        TestMinecart minecart = minecart();
        NbtMap tag = NbtMap.builder()
                .putBoolean("CustomDisplayTile", true)
                .putCompound("DisplayBlock", NbtMap.builder().putInt("val", 1).build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> minecart.loadAdditionalData(tag));
    }

    @Test
    void malformedDisplayBlockFailsWithoutFlag() {
        TestMinecart minecart = minecart();
        NbtMap tag = NbtMap.builder()
                .putCompound("DisplayBlock", NbtMap.builder().putString("name", "no_such_block_xyz").build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> minecart.loadAdditionalData(tag));
    }

    @Test
    void abstractMinecartKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityAbstractMinecart.class, CloudAbstractMinecartData.class);
    }
}
