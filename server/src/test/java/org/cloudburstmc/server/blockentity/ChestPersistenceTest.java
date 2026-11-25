package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.blockentity.data.CloudChestData;
import org.cloudburstmc.server.blockentity.data.CloudContainerData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.VanillaChestBlocks;
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

        assertEquals(Set.of("id", "pairx", "pairz", "pairlead", "CustomName"),
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
        assertEquals(11, client.getInt("pairx"));
        assertEquals(-5, client.getInt("pairz"));
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
    void chestDefinitionsKeepCopperVariantsInOneFamilyAndEnderChestsSeparate() {
        Set<BlockType> storageChests = VanillaChestBlocks.blocksFor(BlockEntityTypes.CHEST);
        assertEquals(Set.of(BlockTypes.CHEST, BlockTypes.TRAPPED_CHEST,
                        BlockTypes.COPPER_CHEST, BlockTypes.EXPOSED_COPPER_CHEST, BlockTypes.WEATHERED_COPPER_CHEST,
                        BlockTypes.OXIDIZED_COPPER_CHEST, BlockTypes.WAXED_COPPER_CHEST, BlockTypes.WAXED_EXPOSED_COPPER_CHEST,
                        BlockTypes.WAXED_WEATHERED_COPPER_CHEST, BlockTypes.WAXED_OXIDIZED_COPPER_CHEST),
                storageChests);
        assertEquals(Set.of(BlockTypes.ENDER_CHEST), VanillaChestBlocks.blocksFor(BlockEntityTypes.ENDER_CHEST));

        assertTrue(VanillaChestBlocks.emitsSignal(BlockTypes.TRAPPED_CHEST));
        assertFalse(VanillaChestBlocks.emitsSignal(BlockTypes.CHEST));
        assertSame(SoundTypes.BLOCK_COPPER_CHEST_WEATHERED_OPEN,
                VanillaChestBlocks.lidSound(BlockTypes.WAXED_WEATHERED_COPPER_CHEST, true));
        assertSame(SoundTypes.RANDOM_ENDERCHESTCLOSED, VanillaChestBlocks.lidSound(BlockTypes.ENDER_CHEST, false));
    }

    @Test
    void pairedChestKeepsEachHalfsStorageAndSharesTheJoinedInventory() {
        ChestBlockEntity left = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        Vector3i rightPosition = POSITION.add(1, 0, 0);
        CloudChunk chunk = left.getChunk();
        BlockState state = BlockStates.CHEST.withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        chunk.setBlockState(POSITION.getX() & 15, POSITION.getY(), POSITION.getZ() & 15, BlockLayer.PRIMARY, state);
        chunk.setBlockState(rightPosition.getX() & 15, rightPosition.getY(), rightPosition.getZ() & 15,
                BlockLayer.PRIMARY, state);
        ChestBlockEntity right = new ChestBlockEntity(BlockEntityTypes.CHEST, chunk, rightPosition);

        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND, 5);
        ItemStack stick = ItemStack.from(ItemTypes.STICK, 2);
        left.getBlockInventory().setItem(3, diamond);
        right.getBlockInventory().setItem(5, stick);

        assertTrue(left.pairWith(right));
        ItemContainer joined = left.getInventory();
        assertSame(joined, right.getInventory());
        assertEquals(54, joined.size());
        assertEquals(stick, joined.getItem(5));
        assertEquals(diamond, joined.getItem(30));
        assertEquals(27, left.getBlockInventory().size());

        ItemStack gold = ItemStack.from(ItemTypes.GOLD_INGOT);
        joined.setItem(30, gold);
        assertEquals(gold, left.getBlockInventory().getItem(3));

        left.unpair();
        assertThrows(IllegalStateException.class, () -> joined.setItem(30, diamond));
        assertNull(left.getPair());
        assertNull(right.getPair());
        assertSame(left.getBlockInventory(), left.getInventory());
        assertEquals(stick, right.getInventory().getItem(5));
    }

    @Test
    void copperChestPairKeepsItsInventoryAndClientPairingAcrossOxidation() {
        ChestBlockEntity left = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        Vector3i rightPosition = POSITION.add(1, 0, 0);
        CloudChunk chunk = left.getChunk();
        BlockState copper = BlockTypes.COPPER_CHEST.getDefaultState()
                .withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH);
        BlockState exposed = BlockTypes.EXPOSED_COPPER_CHEST.getDefaultState().copyTraits(copper);
        int x = POSITION.getX() & 15;
        int y = POSITION.getY();
        int z = POSITION.getZ() & 15;
        chunk.setBlockState(x, y, z, BlockLayer.PRIMARY, copper);
        chunk.setBlockState(x + 1, y, z, BlockLayer.PRIMARY, copper);
        ChestBlockEntity right = new ChestBlockEntity(BlockEntityTypes.CHEST, chunk, rightPosition);

        assertTrue(left.pairWith(right));
        ItemContainer joined = left.getInventory();
        ItemStack diamond = ItemStack.from(ItemTypes.DIAMOND);
        joined.setItem(30, diamond);

        chunk.setBlockState(x, y, z, BlockLayer.PRIMARY, exposed);
        assertSame(right, left.getPair());
        chunk.setBlockState(x + 1, y, z, BlockLayer.PRIMARY, exposed);

        assertTrue(left.isValid());
        assertTrue(right.isValid());
        assertSame(joined, left.getInventory());
        assertSame(joined, right.getInventory());
        assertEquals(diamond, joined.getItem(30));
        assertEquals(rightPosition.getX(), left.getClientTag().getInt("pairx"));
        assertEquals(POSITION.getX(), right.getClientTag().getInt("pairx"));
        assertTrue(right.getClientTag().getBoolean("pairlead"));
    }

    @Test
    void chestBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(ChestBlockEntity.class, CloudChestData.class,
                Map.of("pairedInventory", "A joined view of the two block inventories"));
    }

    @Test
    void enderChestDoesNotPersistBlockInventoryOrPairing() {
        EnderChestBlockEntity chest = (EnderChestBlockEntity) PersistenceFixtures.blockEntity(
                BlockEntityTypes.ENDER_CHEST, POSITION);
        chest.loadAdditionalData(chestTag());

        NbtMap saved = chest.getServerTag();
        assertEquals("EnderChest", saved.getString("id"));
        assertEquals(7, saved.getInt("Mystery"));
        for (String key : List.of("Items", "Findable", "LootTable", "LootTableSeed", "pairx", "pairz", "pairlead")) {
            assertFalse(saved.containsKey(key), key);
        }
        DtoStateChecks.assertKeepsNoDuplicateState(EnderChestBlockEntity.class, CloudBlockEntityData.class);
    }

    @Test
    void containerBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(ContainerBlockEntity.class, CloudContainerData.class);
    }
}
