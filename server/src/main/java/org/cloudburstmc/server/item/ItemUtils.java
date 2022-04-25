package org.cloudburstmc.server.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

@UtilityClass
public class ItemUtils {

    private static final CloudItemRegistry registry = CloudItemRegistry.get();

    private static final Cache<ItemStack, NbtMap> ENCODED_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .softValues()
            .build();

    public static NbtMap serializeItem(ItemStack item) {
        return serializeItem(item, -1);
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMapBuilder tag = NbtMap.builder();

        registry.getSerializer(item.getType()).serialize(item, tag);

        if (slot >= 0) {
            tag.putByte("Slot", (byte) slot);
        }

        return tag.build();
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

    public static List<ItemData> toNetwork(Collection<ItemStack> items) {
        return toNetwork(items, true);
    }

    public static List<ItemData> toNetwork(Collection<ItemStack> items, boolean useNetId) {
        List<ItemData> data = new ArrayList<>();
        for (ItemStack item : items) {
            data.add(ItemUtils.toNetwork(item, useNetId));
        }
        return data;
    }

    public static ItemData toNetwork(ItemStack itemStack) {
        return toNetwork(itemStack, true);
    }

    public static ItemData toNetwork(ItemStack item, boolean useNetId) {
        Identifier identifier = item.getNbt().isEmpty() ? item.getType().getId() : Identifier.fromString(item.getNbt().getString("Name"));

        NbtMap tag = item.getNbt();
        short meta;
        if (tag.isEmpty()) {
            meta = 0;
        } else {
            meta = tag.getShort("Damage", (short) 0);
        }
        int id = registry.getRuntimeId(identifier, meta);

        String[] canPlace = item.get(ItemKeys.CAN_PLACE_ON).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        String[] canBreak = item.get(ItemKeys.CAN_DESTROY).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);

        int blockRuntimeId = item.getBlockState().map(CloudBlockRegistry.REGISTRY::getRuntimeId).orElse(0);

        int netId = 0;
        if (useNetId) {
            if (item.getStackNetworkId() == -1) {
                netId = CloudItemRegistry.get().getNextNetId();
                item.stackNetId = netId;
                CloudItemRegistry.get().addNetId(item);
            } else {
                netId = item.getStackNetworkId();
            }
        }
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
                .usingNetId(useNetId)
                .build();
    }

    public static ItemStack fromNetwork(ItemData data) {
        Identifier id = registry.getIdentifier(data.getId());
        ItemType type = ItemType.of(id);

        String[] canBreak = data.getCanBreak();
        String[] canPlace = data.getCanPlace();

        NbtMap tag = data.getTag();
        if (tag == null) {
            tag = NbtMap.EMPTY;
        }

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

            tag = nbt.build();
        }

        ItemStackBuilder builder = ItemStack.builder();
        builder.stackNetworkId(data.getNetId());
        registry.getSerializer(type).deserialize(id, (short) data.getDamage(), data.getCount(), builder, tag);

        return builder.build();
    }

    public static boolean isNull(ItemStack item) {
        return item == null || item == ItemStack.AIR;
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

            try {
                id = registry.fromLegacy(Utils.toInt(data.get("id")), Utils.toInt(data.getOrDefault("damage", 0)));  //try prior format first
            } catch (NumberFormatException | ClassCastException e) {
                id = Identifier.fromString(data.get("id").toString());
            }
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

        return deserializeItem(id, (short) Utils.toInt(data.getOrDefault("damage", 0)), Utils.toInt(data.getOrDefault("count", 1)), tag, blockRuntimeId);
    }

    // -- Used by recipes and crafting
    public static int getItemHash(ItemStack item) {
        return Objects.hash(System.identityHashCode(item.getType().getId()), item.getNbt().getInt("Damage", 0), item.getCount());
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
