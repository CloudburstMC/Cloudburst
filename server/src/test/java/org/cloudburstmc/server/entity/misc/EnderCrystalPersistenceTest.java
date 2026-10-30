package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.EnderCrystalData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the ender-crystal data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class EnderCrystalPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(EnderCrystalData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.ENDER_CRYSTAL));

        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);
        NbtMap beam = NbtMap.builder().putInt("X", 1).putInt("Y", 2).putInt("Z", 3).build();
        NbtMap input = NbtMap.builder()
                .putBoolean("ShowBottom", false)
                .putCompound("BeamTarget", beam)
                .putInt("Mystery", 7)
                .build();
        crystal.loadAdditionalData(input);

        assertFalse(crystal.isShowingBase());
        assertEquals(Vector3i.from(1, 2, 3), crystal.getBeamTarget());

        NbtMap saved = save(crystal);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("ShowBottom").getClass()), "ShowBottom must be a byte");
        assertFalse(saved.getBoolean("ShowBottom"));
        assertEquals(beam, saved.getCompound("BeamTarget"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);

        NbtMap saved = save(crystal);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("ShowBottom", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the base flag and omits the beam target.
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("ShowBottom").getClass()), "ShowBottom must be a byte");
        assertTrue(saved.getBoolean("ShowBottom"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);
        crystal.setShowingBase(false);
        crystal.setBeamTarget(Vector3i.from(1, 2, 3));

        crystal.loadAdditionalData(NbtMap.EMPTY);

        assertFalse(crystal.isShowingBase());
        assertEquals(Vector3i.from(1, 2, 3), crystal.getBeamTarget());

        NbtMap saved = save(crystal);
        assertFalse(saved.getBoolean("ShowBottom"));
        assertEquals(Vector3i.from(1, 2, 3), Vector3i.from(
                saved.getCompound("BeamTarget").getInt("X"),
                saved.getCompound("BeamTarget").getInt("Y"),
                saved.getCompound("BeamTarget").getInt("Z")));
    }

    @Test
    void clearedBeamTargetStaysAbsent() {
        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);
        crystal.setBeamTarget(Vector3i.from(1, 2, 3));
        crystal.setBeamTarget(null);

        assertNull(crystal.getBeamTarget());

        NbtMap saved = save(crystal);
        assertTrue(saved.get("BeamTarget") == null, "BeamTarget must be absent");
    }

    @Test
    void zeroBeamTargetStaysAbsent() {
        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);
        crystal.setBeamTarget(Vector3i.ZERO);

        assertNull(crystal.getBeamTarget());

        NbtMap saved = save(crystal);
        assertTrue(saved.get("BeamTarget") == null, "ZERO beam must be omitted, as today");
    }

    @Test
    void wrongTypedMetadataKeepsCurrent() {
        EntityEnderCrystal crystal = (EntityEnderCrystal) PersistenceFixtures.entity(EntityTypes.ENDER_CRYSTAL);
        crystal.setShowingBase(false);
        crystal.setBeamTarget(Vector3i.from(1, 2, 3));

        crystal.loadAdditionalData(NbtMap.builder()
                .putString("ShowBottom", "no")
                .putString("BeamTarget", "no")
                .build());

        assertFalse(crystal.isShowingBase());
        assertEquals(Vector3i.from(1, 2, 3), crystal.getBeamTarget());
    }

    @Test
    void enderCrystalKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityEnderCrystal.class, EnderCrystalData.class);
        assertFalse(hasDtoField(EnderCrystalData.class, "showBottom"),
                "EnderCrystalData keeps showBottom; state must live in metadata only");
        assertFalse(hasDtoField(EnderCrystalData.class, "beamTarget"),
                "EnderCrystalData keeps beamTarget; state must live in metadata only");
    }

    private static boolean hasDtoField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
