package org.cloudburstmc.server.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.util.Identifier;
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

    public static NbtMap serializeItem(ItemStack item) {
        //TODO only cache the 'tag' compound and not the whole item
        try {
            return ITEM_CACHE.get(item, () -> {
                NbtMapBuilder tag = NbtMap.builder();
                registry.getSerializer(item.getType()).serialize(item, tag);
                return tag.build();
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
            return ItemStack.AIR;
        }

        return deserializeItem(
                Identifier.fromString(tag.getString("Name")),
                tag.getShort("Damage", (short) 0),
                tag.getByte("Count"),
                tag.getCompound("tag", NbtMap.EMPTY)
        );
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag) {
        return deserializeItem(id, damage, amount, tag, -1);
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag, int blockRuntimeId) {
        ItemStackBuilder builder = ItemStack.builder();
        if (blockRuntimeId >= 0) {
            builder.data(ItemKeys.BLOCK_STATE, CloudBlockRegistry.REGISTRY.getBlock(blockRuntimeId));
        }
        ItemType type = CloudItemRegistry.get().getType(id, damage);
        registry.getSerializer(type).deserialize(id, damage, amount, builder, tag);

        return builder.build();
    }
//
//    public static ItemData toNetwork(ItemStack item) {
//
//    }
//
//    public static ItemData toNetworkNetId(ItemStack item) {
//
//    }

    public static List<ItemData> toNetwork(Collection<ItemStack> items) {
        List<ItemData> data = new ArrayList<>();
        for (ItemStack item : items) {
            data.add(ItemUtils.toNetwork(item));
        }
        return data;
    }

    public static ItemData toNetwork(ItemStack item) {
        Identifier identifier = item.getType().getId();
        short meta = 0;

        int id = registry.getRuntimeId(identifier, meta);

        String[] canPlace = new String[0];
        if(item.get(ItemKeys.CAN_PLACE_ON) != null) {
            canPlace = item.get(ItemKeys.CAN_PLACE_ON).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }
        String[] canBreak = new String[0];
        if(item.get(ItemKeys.CAN_DESTROY) != null) {
            canPlace = item.get(ItemKeys.CAN_DESTROY).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }

        int netId = NET_ID_CACHE.getAndIncrement();
        WeakReference<ItemStack> reference = new WeakReference<>(item);
        NET_ID_REFERENCE.put(netId, reference);

        int blockRuntimeId = item.getBlockState().map(CloudBlockRegistry.REGISTRY::getRuntimeId).orElse(0);

        return ItemData.builder()
                .id(id)
                .damage(meta)
                .count(item.getCount())
//                TODO :D
//                .tag(item.getDataTag().isEmpty() ? null : item.getDataTag())
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockRuntimeId(blockRuntimeId)
                .netId(netId)
                .usingNetId(true)
                .build();
    }

    public static ItemStack fromNetwork(ItemData data) {
        if(data.isUsingNetId()) {
            int netId = data.getNetId();

            WeakReference<ItemStack> weakReference = NET_ID_REFERENCE.remove(netId);

            if(weakReference == null || weakReference.get() == null) {
                log.trace("Trying to find cached ItemStack for netId {} but it doesn't exists", netId);
            } else {
                return weakReference.get();
            }
        }

        Identifier id = registry.getIdentifier(data.getId());
        ItemType type = ItemType.of(id);

        NbtMap tag = data.getTag();
        if (tag == null) {
            tag = NbtMap.EMPTY;
        }

        parseBreakPlaceData(data, tag);

        ItemStackBuilder builder = ItemStack.builder();
        registry.getSerializer(type).deserialize(id, (short) data.getDamage(), data.getCount(), builder, tag);

        return builder.build();
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
        return item == null || item == ItemStack.AIR;
    }

//    TODO Recipe Implementation (version 0.x.x)
//    public static ItemStack fromJson(Map<String, Object> data) {
//        String nbt = (String) data.get("nbt_b64");
//
//        byte[] nbtBytes = null;
//        if (nbt != null) {
//            nbtBytes = Base64.getDecoder().decode(nbt);
//        } else if ((nbt = (String) data.getOrDefault("nbt_hex", null)) != null) { // Support old format for backwards compat
//            nbtBytes = Utils.parseHexBinary(nbt);
//        }
//
//        NbtMap tag;
//        if (nbtBytes != null) {
//            try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
//                tag = (NbtMap) stream.readTag();
//            } catch (IOException e) {
//                throw new IllegalStateException("Unable to decode tag", e);
//            }
//        } else {
//            tag = NbtMap.EMPTY;
//        }
//
//        Identifier id;
//        if (data.containsKey("id")) {
//
//            try {
//                id = registry.fromLegacy(Utils.toInt(data.get("id")), Utils.toInt(data.getOrDefault("damage", 0)));  //try prior format first
//            } catch (NumberFormatException | ClassCastException e) {
//                id = Identifier.fromString(data.get("id").toString());
//            }
//        } else {
//            id = registry.fromLegacy(Utils.toInt(data.get("legacyId")), Utils.toInt(data.getOrDefault("damage", 0)));
//        }
//
//        if (id == null) {
//            throw new IllegalStateException("Unable to decode item JSON");
//        }
//        int blockRuntimeId = -1;
//        if (data.containsKey("blockRuntimeId")) {
//            blockRuntimeId = Utils.toInt(data.get("blockRuntimeId"));
//        }
//
//        return deserializeItem(id, (short) Utils.toInt(data.getOrDefault("damage", 0)), Utils.toInt(data.getOrDefault("count", 1)), tag, blockRuntimeId);
//    }

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
