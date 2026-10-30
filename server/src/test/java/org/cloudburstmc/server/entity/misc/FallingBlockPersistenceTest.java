package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.FallingBlockData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the falling-block data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class FallingBlockPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static NbtMap stateTag(BlockState state) {
        // Today's save writes the definition's state compound ({name, states}), not the
        // full serialized form (which carries a version int).
        return BlockPalette.INSTANCE.getDefinition(state).getState();
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(FallingBlockData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.FALLING_BLOCK));

        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);
        NbtMap state = stateTag(BlockStates.SAND);
        NbtMap input = NbtMap.builder()
                .putCompound("BlockState", state)
                .putInt("Time", 5)
                .putBoolean("DropItem", false)
                .putBoolean("CancelDrop", true)
                .putBoolean("HurtEntities", true)
                .putFloat("DamagePerBlock", 2.5f)
                .putInt("MaximumDamage", 10)
                .putBoolean("AutoExpire", false)
                .putString("LandingSound", "minecraft:random.fizz")
                .putString("BreakSound", "minecraft:random.fizz")
                .putInt("Mystery", 7)
                .build();
        block.loadAdditionalData(input);

        assertEquals(BlockStates.SAND, block.getBlockState());
        assertFalse(block.doesDropItem());
        assertTrue(block.isDropCancelled());
        assertTrue(block.canHurtEntities());
        assertEquals(2.5f, block.getDamagePerBlock());
        assertEquals(10, block.getMaximumDamage());
        assertFalse(block.doesAutoExpire());

        NbtMap saved = save(block);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
        assertFalse(saved.getCompound("BlockState").containsKey("version"),
                "BlockState must keep today's {name, states} form with no version");
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);

        NbtMap saved = save(block);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("BlockState", "Time", "DropItem", "CancelDrop", "HurtEntities",
                "DamagePerBlock", "MaximumDamage", "AutoExpire", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes sand, zero time and damage, drops and expiry on,
        // a 40-point cap, and no sounds.
        assertEquals(stateTag(BlockStates.SAND), saved.getCompound("BlockState"));
        assertFalse(saved.getCompound("BlockState").containsKey("version"),
                "BlockState must keep today's {name, states} form with no version");
        assertTrue(saved.get("Time") instanceof Integer, "Time must be an int");
        assertEquals(0, saved.getInt("Time"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("DropItem").getClass()), "DropItem must be a byte");
        assertTrue(saved.getBoolean("DropItem"));
        assertFalse(saved.getBoolean("CancelDrop"));
        assertFalse(saved.getBoolean("HurtEntities"));
        assertTrue(saved.get("DamagePerBlock") instanceof Float, "DamagePerBlock must be a float");
        assertEquals(0f, saved.getFloat("DamagePerBlock"));
        assertTrue(saved.get("MaximumDamage") instanceof Integer, "MaximumDamage must be an int");
        assertEquals(40, saved.getInt("MaximumDamage"));
        assertTrue(saved.getBoolean("AutoExpire"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);
        BlockState custom = BlockStates.DIRT;
        block.setBlockState(custom);
        block.setDropCancelled(true);
        block.setHurtEntities(true);
        block.setDamagePerBlock(2.0f);

        // The block state key is required, so the absent-keys tag keeps it.
        // Time, DropItem, CancelDrop, HurtEntities, DamagePerBlock, MaximumDamage
        // and AutoExpire have no presence bit: they always fall back to today's
        // defaults, exactly as the get-with-default loads do. Only the sounds
        // keep state when absent.
        block.loadAdditionalData(NbtMap.builder()
                .putCompound("BlockState", stateTag(custom))
                .build());

        NbtMap saved = save(block);
        assertEquals(stateTag(custom), saved.getCompound("BlockState"));
        assertFalse(saved.getCompound("BlockState").containsKey("version"),
                "BlockState must keep today's {name, states} form with no version");
        assertTrue(saved.getBoolean("DropItem"));
        assertFalse(saved.getBoolean("CancelDrop"));
        assertFalse(saved.getBoolean("HurtEntities"));
        assertEquals(0f, saved.getFloat("DamagePerBlock"));
        assertEquals(40, saved.getInt("MaximumDamage"));
        assertTrue(saved.getBoolean("AutoExpire"));
    }

    @Test
    void absentTimeResetsToZero() {
        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);
        block.loadAdditionalData(NbtMap.builder()
                .putCompound("BlockState", stateTag(BlockStates.SAND))
                .putInt("Time", 5)
                .build());

        // Time has no presence bit: it always falls back to zero, exactly as the
        // get-with-default load did.
        block.loadAdditionalData(NbtMap.builder()
                .putCompound("BlockState", stateTag(BlockStates.SAND))
                .build());

        NbtMap saved = save(block);
        assertEquals(0, saved.getInt("Time"));
    }

    @Test
    void absentSoundsLeaveStateUnchanged() {
        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);
        block.loadAdditionalData(NbtMap.builder()
                .putCompound("BlockState", stateTag(BlockStates.SAND))
                .putString("LandingSound", "minecraft:random.fizz")
                .putString("BreakSound", "minecraft:random.fizz")
                .build());

        block.loadAdditionalData(NbtMap.builder()
                .putCompound("BlockState", stateTag(BlockStates.SAND))
                .build());

        NbtMap saved = save(block);
        assertEquals("minecraft:random.fizz", saved.getString("LandingSound"));
        assertEquals("minecraft:random.fizz", saved.getString("BreakSound"));
    }

    @Test
    void missingBlockStateCloses() {
        EntityFallingBlock block = (EntityFallingBlock) PersistenceFixtures.entity(EntityTypes.FALLING_BLOCK);

        block.loadAdditionalData(NbtMap.builder().putInt("Time", 5).build());

        assertTrue(block.isClosed());
    }

    @Test
    void fallingBlockKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityFallingBlock.class, FallingBlockData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityFallingBlock.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("blockState", "landingSound", "breakSound", "time", "dropItem", "cancelDrop", "hurtEntities", "damagePerBlock", "maximumDamage", "autoExpire")) {
            assertFalse(fields.contains(removed),
                    "EntityFallingBlock keeps " + removed + "; state must live in FallingBlockData only");
        }
    }
}