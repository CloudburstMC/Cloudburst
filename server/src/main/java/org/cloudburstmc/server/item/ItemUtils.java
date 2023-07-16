package org.cloudburstmc.server.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
@Log4j2
public class ItemUtils {

    private static final CloudItemRegistry registry = CloudItemRegistry.get();
    private static final AtomicInteger NET_ID_CACHE = new AtomicInteger();
    private static final Int2ReferenceMap<WeakReference<ItemStack>> NET_ID_REFERENCE = new Int2ReferenceOpenHashMap<>();
    private static final Cache<ItemStack, NbtMap> ITEM_CACHE = CacheBuilder.newBuilder().weakKeys().softValues().build();

    public static Optional<ItemStack> getFromNetworkId(int netId) {
        //TODO Should this be a get or a remove?
        return Optional.ofNullable(NET_ID_REFERENCE.remove(netId).get());
    }

    public static NbtMap serializeItem(ItemStack item) {
        NbtMapBuilder nbtTag = NbtMap.builder();

        nbtTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getCount())
                .putShort("Damage", (short) 0);

        if (item.isBlock()) {
            NbtMapBuilder blockTag = NbtMap.builder();
            BlockState blockState = item.get(ItemKeys.BLOCK_STATE);

//            log.info(item.getType() + " - " + blockState + " - " + BlockPalette.INSTANCE.getIdentifier(blockState));
            blockTag.putString("Name", BlockPalette.INSTANCE.getIdentifier(blockState).toString());
            blockTag.putShort("Damage", (short) 0);

            nbtTag.put("Block", blockTag.build());
        }

        if (item.get(ItemKeys.CAN_DESTROY) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_DESTROY)
                    .stream()
                    .map(BlockPalette.INSTANCE::getTypeIdentifiers)
                    .flatMap(Collection::stream)
                    .map(Identifier::toString)
                    .toList();

            nbtTag.putList("CanDestroy", NbtType.STRING, blocks);
        }

        if (item.get(ItemKeys.CAN_PLACE_ON) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_PLACE_ON).stream().map(blockType -> blockType.getId().toString()).toList();
            nbtTag.putList("CanPlaceOn", NbtType.STRING, blocks);
        }

        NbtMap tag = ItemUtils.getSerializedTag(item);
        if (!tag.isEmpty()) {
            nbtTag.put("tag", tag);
        }

        return nbtTag.build();
    }

    private static NbtMap getSerializedTag(ItemStack item) {
        try {
            return ITEM_CACHE.get(item, () -> {
                NbtMapBuilder tagBuilder = NbtMap.builder();
                registry.getSerializer(item.getType()).serialize(item, tagBuilder);
                return tagBuilder.build();
            });
        } catch (ExecutionException e) {
            throw new IllegalStateException("Invalid state while serializing item " + item, e);
        }
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMap map = serializeItem(item);

        if (slot >= 0) {
            map.toBuilder().putByte("Slot", (byte) slot).build();
        }

        return map;
    }

    public static ItemStack deserializeItem(NbtMap tag) {
        if (!tag.containsKey("Name", NbtType.STRING) || !tag.containsKey("Count", NbtType.BYTE)) {
            return ItemStack.EMPTY;
        }

        ItemStackBuilder builder = ItemUtils.deserializeItem(
                Identifier.parse(tag.getString("Name")),
                tag.getShort("Damage", (short) 0),
                tag.getByte("Count"),
                tag.getCompound("tag", NbtMap.EMPTY)).toBuilder();

        if (tag.containsKey("CanPlaceOn", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanPlaceOn", NbtType.STRING, Collections.emptyList()).stream().map(Identifier::parse).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_PLACE_ON, list);
        }

        if (tag.containsKey("CanDestroy", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanDestroy", NbtType.STRING, Collections.emptyList()).stream().map(Identifier::parse).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_DESTROY, list);
        }

        return builder.build();
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag) {
        ItemStackBuilder builder = ItemStack.builder();
        if (amount > 0) {
            ItemType type = CloudItemRegistry.get().getType(id, damage);

//            log.info(id + " - " + damage + " > " + type + " - " + type.getId());
            builder.itemType(type);
            builder.amount(amount);

            if (type instanceof BlockType) {
                BlockState blockState = BlockStateMetaMappings.getStateFromMeta(id, damage);

                if (blockState != null) {
                    builder.data(ItemKeys.BLOCK_STATE, blockState);
                }
            }

            registry.getSerializer(type).deserialize(id, damage, builder, tag);
        } else {
            builder.itemType(BlockTypes.AIR);
        }
        return builder.build();
    }

    public static ItemData toNetwork(ItemStack item) {
        return ItemUtils.toNetworkBuilder(item)
                .netId(0)
                .usingNetId(false)
                .build();
    }

    public static List<ItemData> toNetwork(Collection<ItemStack> item) {
        List<ItemData> data = new ArrayList<>();

        for (ItemStack itemStack : item) {
            data.add(toNetwork(itemStack));
        }

        return data;
    }

    public static List<ItemDescriptorWithCount> toDescriptors(Collection<ItemStack> item) {
        List<ItemDescriptorWithCount> data = new ArrayList<>();

        for (ItemStack itemStack : item) {
            data.add(toDescriptor(itemStack));
        }

        return data;
    }

    public static ItemDescriptorWithCount toDescriptor(ItemStack item) {
        return ItemDescriptorWithCount.fromItem(toNetwork(item));
    }

    public static ItemData toNetworkNetId(ItemStack item) {
        int netId = NET_ID_CACHE.getAndIncrement();
        WeakReference<ItemStack> reference = new WeakReference<>(item);
        NET_ID_REFERENCE.put(netId, reference);

        return ItemUtils.toNetworkBuilder(item)
                .netId(netId)
                .usingNetId(true)
                .build();
    }

    private static ItemData.Builder toNetworkBuilder(ItemStack item) {
        Identifier identifier = item.getType().getId();
        ItemDefinition definition = registry.getDefinition(identifier);

        String[] canPlace = new String[0];
        if (item.get(ItemKeys.CAN_PLACE_ON) != null) {
            canPlace = item.get(ItemKeys.CAN_PLACE_ON).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }
        String[] canBreak = new String[0];
        if (item.get(ItemKeys.CAN_DESTROY) != null) {
            canPlace = item.get(ItemKeys.CAN_DESTROY).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }

        CloudBlockDefinition blockDefinition = item.getBlockState().map(CloudBlockRegistry.REGISTRY::getDefinition).orElse(null);
        NbtMap tag = ItemUtils.getSerializedTag(item);

        return ItemData.builder()
                .definition(definition)
                .damage(0)
                .count(item.getCount())
                .tag(tag.isEmpty() ? null : tag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockDefinition(blockDefinition);
    }

    public static ItemStack fromNetwork(ItemData data) {
        if (data.isUsingNetId()) {
            int netId = data.getNetId();

            WeakReference<ItemStack> weakReference = NET_ID_REFERENCE.remove(netId);

            if (weakReference == null || weakReference.get() == null) {
                log.trace("Trying to find cached ItemStack for netId {} but it doesn't exists", netId);
            } else {
                return weakReference.get();
            }
        }

        Identifier id = Identifier.parse(data.getDefinition().getIdentifier());
        NbtMap tag = data.getTag();
        if (tag == null) {
            tag = NbtMap.EMPTY;
        }

        parseBreakPlaceData(data, tag);

        return ItemUtils.deserializeItem(id, (short) 0, data.getCount(), tag);
    }

    private static void parseBreakPlaceData(ItemData data, NbtMap tag) {
        String[] canBreak = data.getCanBreak();
        String[] canPlace = data.getCanPlace();

        if (canBreak.length > 0 || canPlace.length > 0) {
            NbtMapBuilder nbt = tag.toBuilder();

            if (canBreak.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canBreak));
                nbt.putList("CanDestroy", NbtType.STRING, listTag);
            }

            if (canPlace.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canPlace));
                nbt.putList("CanPlaceOn", NbtType.STRING, listTag);
            }

            tag.putAll(nbt);
        }
    }

    public static boolean isNull(ItemStack item) {
        return item == null || item == ItemStack.EMPTY;
    }

    public static ItemStack fromJson(Map<String, Object> data) {
        String nbt = (String) data.get("nbt_b64");

        byte[] nbtBytes = null;
        if (nbt != null) {
            nbtBytes = Base64.getDecoder().decode(nbt);
        } else if ((nbt = (String) data.getOrDefault("nbt_hex", null)) != null) { // Support old format for backwards compat
            nbtBytes = Utils.parseHexBinary(nbt);
        }

        NbtMap tag;
        if (nbtBytes != null) {
            try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
                tag = (NbtMap) stream.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to decode tag", e);
            }
        } else {
            tag = NbtMap.EMPTY;
        }

        Identifier id;
        if (data.containsKey("id")) {
//            try {
//                id = registry.fromLegacy(Utils.toInt(data.get("id")), Utils.toInt(data.getOrDefault("damage", 0)));  //try prior format first
//            } catch (NumberFormatException | ClassCastException e) {
//                id = Identifier.fromString(data.get("id").toString());
//            }
            id = Identifier.parse(data.get("id").toString());
        } else {
            id = registry.fromLegacy(Utils.toInt(data.get("legacyId")), Utils.toInt(data.getOrDefault("damage", 0)));
        }

        if (id == null) {
            throw new IllegalStateException("Unable to decode item JSON");
        }
        int blockRuntimeId = -1;
        if (data.containsKey("blockRuntimeId")) {
            blockRuntimeId = Utils.toInt(data.get("blockRuntimeId"));
        }

        return deserializeItem(id, (short) Utils.toInt(data.getOrDefault("damage", 0)), Utils.toInt(data.getOrDefault("count", 1)), tag/*, blockRuntimeId*/);
    }

    // -- Used by recipes and crafting
    public static int getItemHash(ItemStack item) {
        return Objects.hash(System.identityHashCode(item.getType().getId()), item.getCount());
    }

    public static UUID getMultiItemHash(List<ItemStack> items) {
        ByteBuffer buffer = ByteBuffer.allocate(items.size() * 8);
        for (ItemStack item : items) {
            if (item != null)
                buffer.putInt(getItemHash((ItemStack) item));
        }
        return UUID.nameUUIDFromBytes(buffer.array());
    }
}
