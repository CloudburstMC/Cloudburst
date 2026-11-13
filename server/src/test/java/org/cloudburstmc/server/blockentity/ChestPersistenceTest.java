package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudChestData;
import org.cloudburstmc.server.blockentity.data.CloudContainerData;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChestPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 27;

    private static List<NbtMap> fullItems() {
        List<NbtMap> items = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack;
            if (slot == 3) {
                stack = ItemStack.from(ItemTypes.DIAMOND, 5);
            } else if (slot == 5) {
                stack = ItemStack.from(ItemTypes.STICK, 2);
            } else {
                stack = ItemStack.EMPTY;
            }
            items.add(ItemStackNbtCodec.serializeItem(stack, slot));
        }
        return items;
    }

    private static NbtMap chestTag() {
        return NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Loot chest")
                .putList("Items", NbtType.COMPOUND, fullItems())
                .putBoolean("Findable", true)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .putInt("pairx", 11)
                .putInt("pairz", -5)
                .putBoolean("pairlead", true)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {

        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        NbtMap input = chestTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(3));
        assertTrue(entity.isFindable());
        assertEquals("minecraft:chests/simple_dungeon", entity.getLootTable());
        assertEquals(12345, entity.getLootTableSeed());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        assertEquals(Set.of("id", "pairx", "pairz", "pairlead"),
                entity.getClientTag().keySet());
    }

    @Test
    void emptyChestWritesAirForEverySlot() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtList<?> items = assertInstanceOf(NbtList.class, entity.getServerTag().get("Items"));
        assertEquals(NbtType.COMPOUND, items.getType());
        assertEquals(SIZE, items.size());
        for (int slot = 0; slot < SIZE; slot++) {
            assertEquals(ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, slot), items.get(slot),
                    "slot " + slot);
        }

        for (int slot = 0; slot < SIZE; slot++) {
            assertTrue(entity.getItem(slot).isEmpty(), "slot " + slot);
        }
    }

    @Test
    void badItemFailsLoadInsteadOfEmptyingChest() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        NbtMap badStew = NbtMap.builder()
                .putString("Name", "minecraft:suspicious_stew")
                .putShort("Damage", (short) 99)
                .putByte("Count", (byte) 1)
                .putByte("Slot", (byte) 0)
                .build();
        NbtMap tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putList("Items", NbtType.COMPOUND, List.of(badStew))
                .build();

        assertThrows(Exception.class, () -> entity.loadAdditionalData(tag));
    }

    @Test
    void missingSlotBindsSlotZero() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 5);

        NbtMapBuilder entryBuilder = NbtMap.builder();
        entryBuilder.putAll(ItemStackNbtCodec.serializeItem(diamond));
        NbtMap entry = entryBuilder.build();
        assertFalse(entry.containsKey("Slot"));
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putList("Items", NbtType.COMPOUND, List.of(entry))
                .build());

        assertEquals(diamond, entity.getItem(0));
    }

    @Test
    void shortSlotBindsSlotZero() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 5);

        NbtMapBuilder entryBuilder = ItemStackNbtCodec.serializeItem(diamond, 5).toBuilder();
        entryBuilder.putShort("Slot", (short) 5);
        NbtMap entry = entryBuilder.build();
        assertTrue(entry.get("Slot") instanceof Short);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putList("Items", NbtType.COMPOUND, List.of(entry))
                .build());

        assertEquals(diamond, entity.getItem(0));
    }

    @Test
    void outOfRangeSlotFailsLoadWithoutTouchingContainer() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 5);
        entity.setItem(1, diamond);

        NbtMap tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putList("Items", NbtType.COMPOUND, List.of(ItemStackNbtCodec.serializeItem(diamond, SIZE)))
                .build();

        tools.jackson.core.JacksonException thrown =
                assertThrows(tools.jackson.core.JacksonException.class, () -> entity.loadAdditionalData(tag));
        boolean bare = false;
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof IndexOutOfBoundsException) {
                bare = true;
            }
        }
        assertTrue(bare, "out-of-range slot must fail with a bare exception, not a kind mismatch");
        assertEquals(diamond, entity.getItem(1));
    }

    @Test
    void nonLeadPairedChestKeepsPairKeys() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putInt("pairx", 11)
                .putInt("pairz", -5)
                .putBoolean("pairlead", false)
                .build());

        assertTrue(entity.isPaired());

        NbtMap saved = entity.getServerTag();
        assertTrue(saved.get("pairx") instanceof Integer);
        assertEquals(11, saved.getInt("pairx"));
        assertTrue(saved.get("pairz") instanceof Integer);
        assertEquals(-5, saved.getInt("pairz"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("pairlead").getClass()));
        assertFalse(saved.getBoolean("pairlead"));

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("pairx"));
        assertFalse(client.containsKey("pairz"));
        assertFalse(client.containsKey("pairlead"));
    }

    @Test
    void unpairedChestDoesNotResurrectPair() throws Exception {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(chestTag());
        assertTrue(entity.isPaired());

        entity.data().setPairPosition(null);
        assertFalse(entity.isPaired());

        NbtMap saved = entity.getServerTag();
        assertFalse(saved.containsKey("pairx"));
        assertFalse(saved.containsKey("pairz"));
        assertFalse(saved.containsKey("pairlead"));

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("pairx"));
        assertFalse(client.containsKey("pairz"));
        assertFalse(client.containsKey("pairlead"));
    }

    @Test
    void lootTableRoundTrips() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .build());

        assertEquals("minecraft:chests/simple_dungeon", entity.getLootTable());
        assertEquals(12345, entity.getLootTableSeed());

        NbtMap saved = entity.getServerTag();
        assertEquals("minecraft:chests/simple_dungeon", saved.getString("LootTable"));
        assertTrue(saved.get("LootTableSeed") instanceof Integer);
        assertEquals(12345, saved.getInt("LootTableSeed"));
        assertFalse(entity.getClientTag().containsKey("LootTable"));
        assertFalse(entity.getClientTag().containsKey("LootTableSeed"));

        ChestBlockEntity plain = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        plain.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertNull(plain.getLootTable());
        assertNull(plain.getLootTableSeed());
        assertFalse(plain.getServerTag().containsKey("LootTable"));
        assertFalse(plain.getServerTag().containsKey("LootTableSeed"));
    }

    @Test
    void pairedChestCombinedContainerSeesDataContainers() {
        ChestBlockEntity left = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST,
                Vector3i.from(10, 64, -5));
        ChestBlockEntity right = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST,
                Vector3i.from(11, 64, -5));

        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 5);
        ItemStack stick = ItemStack.from(ItemTypes.STICK, 2);
        left.data().getItems().setItem(3, diamond);
        right.data().getItems().setItem(5, stick);

        left.data().setPairPosition(right.getPosition());
        right.data().setPairPosition(left.getPosition());
        left.setPairlead(true);
        left.createCombinedContainer(right);

        CloudContainer combined = left.getContainer();
        assertEquals(54, combined.size());

        int diamondSlot = -1;
        int stickSlot = -1;
        for (int slot = 0; slot < combined.size(); slot++) {
            if (combined.getItem(slot).equals(diamond)) {
                diamondSlot = slot;
            }
            if (combined.getItem(slot).equals(stick)) {
                stickSlot = slot;
            }
        }
        assertNotEquals(-1, diamondSlot, "combined container sees the left chest's DTO container");
        assertNotEquals(-1, stickSlot, "combined container sees the right chest's DTO container");
        assertNotEquals(diamondSlot, stickSlot);

        ItemStack gold = ItemStack.from(ItemTypes.GOLD_INGOT, 1);
        combined.setItem(diamondSlot, gold);
        assertEquals(gold, left.data().getItems().getItem(3));

        combined.setItem(stickSlot, ItemStack.EMPTY);
        assertTrue(right.data().getItems().getItem(5).isEmpty());

        ItemStack iron = ItemStack.from(ItemTypes.IRON_INGOT, 4);
        right.data().getItems().setItem(5, iron);
        assertEquals(iron, combined.getItem(stickSlot));

        ItemStack coal = ItemStack.from(ItemTypes.COAL, 7);
        left.data().getItems().setItem(3, coal);
        assertEquals(coal, combined.getItem(diamondSlot));
    }

    @Test
    void unpairedPartnerIsRepairedBeforeCombining() {
        final ChestBlockEntity[] leftRef = new ChestBlockEntity[1];
        final ChestBlockEntity[] rightRef = new ChestBlockEntity[1];
        ChestBlockEntity seed = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST,
                Vector3i.from(10, 64, -5));
        org.cloudburstmc.server.level.chunk.CloudChunk chunk = seed.getChunk();
        seed.close();
        ChestBlockEntity left = new ChestBlockEntity(BlockEntityTypes.CHEST, chunk,
                Vector3i.from(10, 64, -5)) {
            @Override
            public ChestBlockEntity getPair() {
                return rightRef[0];
            }
        };
        ChestBlockEntity right = new ChestBlockEntity(BlockEntityTypes.CHEST, chunk,
                Vector3i.from(11, 64, -5)) {
            @Override
            public ChestBlockEntity getPair() {
                return leftRef[0];
            }
        };
        leftRef[0] = left;
        rightRef[0] = right;

        left.data().setPairPosition(right.getPosition());
        assertFalse(right.isPaired(), "partner starts unpaired");

        left.checkPairing();

        assertEquals(right.getPosition(), left.data().getPairPosition());
        assertEquals(left.getPosition(), right.data().getPairPosition());
        assertTrue(left.isPaired());
        assertTrue(right.isPaired());
        assertEquals(54, left.getContainer().size());
        assertEquals(54, right.getContainer().size());
    }

    @Test
    void chestBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(ChestBlockEntity.class, CloudChestData.class,
                Map.of("combinedContainer",
                        "derived double-chest view over the two chests' DTO containers, not persisted state"));
    }

    @Test
    void enderChestBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(EnderChestBlockEntity.class, CloudChestData.class,
                Map.of("combinedContainer",
                        "inherited from ChestBlockEntity: derived double-chest view over the two chests' DTO containers, not persisted state"));
    }

    @Test
    void containerBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(ContainerBlockEntity.class, CloudContainerData.class);
    }
}
