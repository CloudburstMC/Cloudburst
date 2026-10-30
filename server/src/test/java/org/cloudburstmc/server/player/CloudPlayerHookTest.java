package org.cloudburstmc.server.player;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the production player save and load hook bodies
 * ({@link CloudPlayer#loadHookKeys} / {@link CloudPlayer#saveHookKeys}) directly.
 * A full {@code CloudPlayer} needs a live server, so these tests run the extracted
 * hook bodies — the same code the hooks call — against holder objects, combined
 * with the production mapper write the base save runs first. Every key below is
 * hook- or data-owned, so no raw pass-through is involved and this is the full
 * production save for these keys.
 */
class CloudPlayerHookTest {

    private static org.cloudburstmc.api.entity.data.PlayerData newData() {
        return new org.cloudburstmc.api.entity.data.PlayerData();
    }

    /**
     * Emulates the production save order for the hook keys: the base writes the
     * data instance first, then the save hook adds its keys.
     */
    private static NbtMap save(org.cloudburstmc.api.entity.data.PlayerData data, PlayerData holder,
                               CloudPlayer.FoodHookState food) {
        NbtMapBuilder tag = NbtMap.builder();
        tag.putAll(ServerNbt.MAPPER.write(data));
        CloudPlayer.saveHookKeys(tag, holder, food, null);
        return tag.build();
    }

    @Test
    void newPlayerSaveWritesCreditsZero() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        // No stored tag: an empty load leaves the defaults alone.
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.EMPTY, holder, data);
        NbtMap saved = save(data, holder, food);

        // A new player has no stored seenCredits, yet every save writes it as a byte.
        assertTrue(saved.get("seenCredits") instanceof Byte, "seenCredits must be a byte");
        assertEquals((byte) 0, saved.get("seenCredits"));
        // Experience and food are always written from today's defaults.
        assertTrue(saved.get("EXP") instanceof Integer, "EXP must be an int");
        assertEquals(0, saved.getInt("EXP"));
        assertEquals(20, saved.getInt("foodLevel"));
        assertEquals(5.0f, saved.getFloat("foodSaturationLevel"));
        assertEquals(0.0f, saved.getFloat("foodExhaustionLevel"));
        assertEquals(0, saved.getInt("foodTickTimer"));
        // Nothing else is set yet.
        assertFalse(saved.containsKey("firstPlayed"));
        assertFalse(saved.containsKey("lastPlayed"));
        assertFalse(saved.containsKey("Level"));
        assertFalse(saved.containsKey("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertFalse(saved.containsKey("SpawnYaw"));
        assertFalse(saved.containsKey("playerGameType"));
    }

    @Test
    void partialTimestampsWriteBackAsLoaded() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();

        // firstPlayed without lastPlayed is written back with its loaded value.
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(
                NbtMap.builder().putLong("firstPlayed", 1000L).build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertTrue(saved.get("firstPlayed") instanceof Long, "firstPlayed must be a long");
        assertEquals(1000L, saved.getLong("firstPlayed"));
        assertFalse(saved.containsKey("lastPlayed"));

        // Either half at zero is still written back, not dropped.
        data = newData();
        holder = new PlayerData();
        food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putLong("firstPlayed", 500L)
                .putLong("lastPlayed", 0L)
                .build(), holder, data);
        saved = save(data, holder, food);
        assertEquals(500L, saved.getLong("firstPlayed"));
        assertTrue(saved.containsKey("lastPlayed"));
        assertEquals(0L, saved.getLong("lastPlayed"));
    }

    @Test
    void partialSpawnWritesBackAsLoaded() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();

        // A spawn level without coordinates is written back on its own.
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(
                NbtMap.builder().putString("SpawnLevel", "world").build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertEquals("world", saved.getString("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertFalse(saved.containsKey("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
        assertFalse(saved.containsKey("SpawnYaw"));

        // Coordinates without a spawn level are written back on their own.
        data = newData();
        holder = new PlayerData();
        food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .build(), holder, data);
        saved = save(data, holder, food);
        assertFalse(saved.containsKey("SpawnLevel"));
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertEquals(-3, saved.getInt("SpawnZ"));
        assertFalse(saved.containsKey("SpawnYaw"));

        // A lone spawn yaw is written back on its own.
        data = newData();
        holder = new PlayerData();
        food = CloudPlayer.loadHookKeys(NbtMap.builder().putFloat("SpawnYaw", 90.0f).build(), holder, data);
        saved = save(data, holder, food);
        assertFalse(saved.containsKey("SpawnLevel"));
        assertTrue(saved.get("SpawnYaw") instanceof Float, "SpawnYaw must be a float");
        assertEquals(90.0f, saved.getFloat("SpawnYaw"));
    }

    @Test
    void loneSpawnCoordinateWritesBack() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();

        // A lone SpawnX was kept and rewritten at 62b3ea0b0: the three spawn
        // coordinates were independent Integer fields, so a partial tag kept
        // the good ones when the holder condition failed.
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(
                NbtMap.builder().putInt("SpawnX", 5).build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must be an int");
        assertEquals(5, saved.getInt("SpawnX"));
        assertFalse(saved.containsKey("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void twoSpawnCoordinatesWriteBack() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();

        // SpawnX + SpawnY without SpawnZ are both written back.
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertTrue(saved.get("SpawnY") instanceof Integer, "SpawnY must be an int");
        assertEquals(64, saved.getInt("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void mistypedSpawnZKeepsGoodCoordinates() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();

        // A mistyped SpawnZ next to good X/Y keeps the good coordinates.
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putString("SpawnZ", "high")
                .build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
    }

    @Test
    void savedBaselineRefreshesWriteBack() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putString("SpawnLevel", "world")
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .putFloat("SpawnYaw", 90.0f)
                .build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertEquals(1, saved.getInt("SpawnX"));

        // A runtime move while the group is complete saves from the holder and
        // refreshes the remembered baseline.
        holder.setSpawnLocation(Vector3i.from(10, 70, 30));
        saved = save(data, holder, food);
        assertEquals(10, saved.getInt("SpawnX"));
        assertEquals(70, saved.getInt("SpawnY"));
        assertEquals(30, saved.getInt("SpawnZ"));

        // Clearing the level afterwards writes back the refreshed values, not the load.
        holder.setSpawnLevel(null);
        saved = save(data, holder, food);
        assertEquals("world", saved.getString("SpawnLevel"));
        assertEquals(10, saved.getInt("SpawnX"));
        assertEquals(70, saved.getInt("SpawnY"));
        assertEquals(30, saved.getInt("SpawnZ"));
        assertEquals(90.0f, saved.getFloat("SpawnYaw"));
    }

    @Test
    void hookRoundTripPreservesEveryKeyAndType() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        NbtMap stored = NbtMap.builder()
                .putInt("EXP", 7)
                .putInt("expLevel", 3)
                .putInt("foodLevel", 14)
                .putFloat("foodSaturationLevel", 3.5f)
                .putFloat("foodExhaustionLevel", 1.25f)
                .putInt("foodTickTimer", 9)
                .putLong("firstPlayed", 1000L)
                .putLong("lastPlayed", 2000L)
                .putString("Level", "world")
                .putString("SpawnLevel", "world")
                .putInt("SpawnX", 1)
                .putInt("SpawnY", 64)
                .putInt("SpawnZ", -3)
                .putFloat("SpawnYaw", 90.0f)
                .putInt("playerGameType", 1)
                .build();

        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        data.setSeenCredits(true);
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(stored, holder, data);

        assertEquals(7, data.getExp());
        assertEquals(3, data.getExpLevel());
        assertEquals(1000L, holder.getFirstPlayed().getAsLong());
        assertEquals(2000L, holder.getLastPlayed().getAsLong());
        assertEquals("world", holder.getLevel());
        assertEquals("world", holder.getSpawnLevel());
        assertEquals(Vector3i.from(1, 64, -3), holder.getSpawnLocation());
        assertEquals(90.0f, holder.getSpawnYaw());
        assertEquals(GameMode.CREATIVE, holder.getGamemode());

        NbtMap saved = save(data, holder, food);
        assertTrue(saved.get("EXP") instanceof Integer, "EXP must be an int");
        assertEquals(7, saved.getInt("EXP"));
        assertTrue(saved.get("expLevel") instanceof Integer, "expLevel must be an int");
        assertEquals(3, saved.getInt("expLevel"));
        assertTrue(saved.get("seenCredits") instanceof Byte, "seenCredits must be a byte");
        assertEquals((byte) 1, saved.get("seenCredits"));
        assertTrue(saved.get("foodLevel") instanceof Integer, "foodLevel must be an int");
        assertEquals(14, saved.getInt("foodLevel"));
        assertTrue(saved.get("foodSaturationLevel") instanceof Float, "foodSaturationLevel must be a float");
        assertEquals(3.5f, saved.getFloat("foodSaturationLevel"));
        assertTrue(saved.get("foodExhaustionLevel") instanceof Float, "foodExhaustionLevel must be a float");
        assertEquals(1.25f, saved.getFloat("foodExhaustionLevel"));
        assertTrue(saved.get("foodTickTimer") instanceof Integer, "foodTickTimer must be an int");
        assertEquals(9, saved.getInt("foodTickTimer"));
        assertTrue(saved.get("firstPlayed") instanceof Long, "firstPlayed must be a long");
        assertEquals(1000L, saved.getLong("firstPlayed"));
        assertTrue(saved.get("lastPlayed") instanceof Long, "lastPlayed must be a long");
        assertEquals(2000L, saved.getLong("lastPlayed"));
        assertEquals("world", saved.getString("Level"));
        assertEquals("world", saved.getString("SpawnLevel"));
        assertTrue(saved.get("SpawnX") instanceof Integer, "SpawnX must be an int");
        assertEquals(1, saved.getInt("SpawnX"));
        assertEquals(64, saved.getInt("SpawnY"));
        assertEquals(-3, saved.getInt("SpawnZ"));
        assertTrue(saved.get("SpawnYaw") instanceof Float, "SpawnYaw must be a float");
        assertEquals(90.0f, saved.getFloat("SpawnYaw"));
        assertTrue(saved.get("playerGameType") instanceof Integer, "playerGameType must be an int");
        assertEquals(1, saved.getInt("playerGameType"));
    }

    @Test
    void mistypedHookKeysStayDropped() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        org.cloudburstmc.api.entity.data.PlayerData data = newData();
        PlayerData holder = new PlayerData();
        // Keys of another kind never bind, so the save omits them instead of
        // passing them through or writing holder defaults.
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.builder()
                .putString("firstPlayed", "soon")
                .putInt("SpawnLevel", 123)
                .putString("SpawnX", "1")
                .putString("foodLevel", "full")
                .build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertFalse(saved.containsKey("firstPlayed"));
        assertFalse(saved.containsKey("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        // Mistyped food falls back to today's defaults, which are always written.
        assertTrue(saved.get("foodLevel") instanceof Integer, "foodLevel must be an int");
        assertEquals(20, saved.getInt("foodLevel"));
    }

    @Test
    void nonByteSlotBindsSlotZero() {
        PersistenceFixtures.ensureInitialized();
        ServerNbt.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        NbtMap base = ItemUtils.serializeItem(diamond);
        NbtMapBuilder shortBuilder = NbtMap.builder();
        shortBuilder.putAll(base);
        shortBuilder.putShort("Slot", (short) 2);
        NbtMapBuilder intBuilder = NbtMap.builder();
        intBuilder.putAll(base);
        intBuilder.putInt("Slot", 2);
        NbtMapBuilder stringBuilder = NbtMap.builder();
        stringBuilder.putAll(base);
        stringBuilder.putString("Slot", "2");
        List<NbtMap> entries = List.of(shortBuilder.build(), intBuilder.build(), stringBuilder.build());
        for (NbtMap entry : entries) {
            // Only a byte Slot tag binds; any other kind lands on slot 0.
            org.cloudburstmc.api.entity.data.PlayerData data = newData();
            data.setInventory(org.cloudburstmc.api.inventory.ItemContainers.detached(36));
            data.setEnderChest(org.cloudburstmc.api.inventory.ItemContainers.detached(27));
            ServerNbt.MAPPER.readInto(NbtMap.builder()
                    .putList("Inventory", NbtType.COMPOUND, List.of(entry))
                    .build(), data);
            assertEquals(diamond, data.getInventory().getItem(0),
                    "a " + entry.get("Slot").getClass().getSimpleName() + " Slot must bind slot 0");
            assertTrue(data.getInventory().getItem(2).isEmpty(),
                    "a " + entry.get("Slot").getClass().getSimpleName() + " Slot must not bind slot 2");
        }
    }
}
