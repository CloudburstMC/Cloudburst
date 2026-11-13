package org.cloudburstmc.server.player;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.data.CloudPlayerData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloudPlayerHookTest {

    private static CloudPlayerData newData() {
        return new CloudPlayerData();
    }

    private static NbtMap save(CloudPlayerData data, PlayerData holder,
                               CloudPlayer.FoodHookState food) {
        NbtMapBuilder tag = NbtMap.builder();
        tag.putAll(ServerNbt.MAPPER.write(data));
        CloudPlayer.saveHookKeys(tag, holder, food, null);
        return tag.build();
    }

    @Test
    void newPlayerSaveWritesCreditsZero() {
        PersistenceFixtures.ensureInitialized();
        CloudPlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(NbtMap.EMPTY, holder, data);
        NbtMap saved = save(data, holder, food);

        assertTrue(saved.get("seenCredits") instanceof Byte, "seenCredits must be a byte");
        assertEquals((byte) 0, saved.get("seenCredits"));
        assertTrue(saved.get("EXP") instanceof Integer, "EXP must be an int");
        assertEquals(0, saved.getInt("EXP"));
        assertEquals(20, saved.getInt("foodLevel"));
        assertEquals(5.0f, saved.getFloat("foodSaturationLevel"));
        assertEquals(0.0f, saved.getFloat("foodExhaustionLevel"));
        assertEquals(0, saved.getInt("foodTickTimer"));
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

        CloudPlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(
                NbtMap.builder().putLong("firstPlayed", 1000L).build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertTrue(saved.get("firstPlayed") instanceof Long, "firstPlayed must be a long");
        assertEquals(1000L, saved.getLong("firstPlayed"));
        assertFalse(saved.containsKey("lastPlayed"));

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

        CloudPlayerData data = newData();
        PlayerData holder = new PlayerData();
        CloudPlayer.FoodHookState food = CloudPlayer.loadHookKeys(
                NbtMap.builder().putString("SpawnLevel", "world").build(), holder, data);
        NbtMap saved = save(data, holder, food);
        assertEquals("world", saved.getString("SpawnLevel"));
        assertFalse(saved.containsKey("SpawnX"));
        assertFalse(saved.containsKey("SpawnY"));
        assertFalse(saved.containsKey("SpawnZ"));
        assertFalse(saved.containsKey("SpawnYaw"));

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

        CloudPlayerData data = newData();
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

        CloudPlayerData data = newData();
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

        CloudPlayerData data = newData();
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
        CloudPlayerData data = newData();
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

        holder.setSpawnLocation(Vector3i.from(10, 70, 30));
        saved = save(data, holder, food);
        assertEquals(10, saved.getInt("SpawnX"));
        assertEquals(70, saved.getInt("SpawnY"));
        assertEquals(30, saved.getInt("SpawnZ"));

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

        CloudPlayerData data = newData();
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
        CloudPlayerData data = newData();
        PlayerData holder = new PlayerData();
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
        assertTrue(saved.get("foodLevel") instanceof Integer, "foodLevel must be an int");
        assertEquals(20, saved.getInt("foodLevel"));
    }

    @Test
    void nonByteSlotBindsSlotZero() {
        PersistenceFixtures.ensureInitialized();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 2);
        NbtMap base = ItemStackNbtCodec.serializeItem(diamond);
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
            CloudPlayerData data = newData();
            data.setInventory(new CloudContainer(36));
            data.setEnderChest(new CloudContainer(27));
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
