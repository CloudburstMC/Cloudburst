package org.cloudburstmc.server.item;

import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@UtilityClass
public class ItemUtils {

    private static final CloudItemRegistry registry = CloudItemRegistry.get();

    public static NbtMap serializeItem(ItemStack item) {
        return serializeItem(item, -1);
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMapBuilder tag = NbtMap.builder();

        registry.getSerializer(item.getType()).serialize((CloudItemStack) item, tag);

        if (slot >= 0) {
            tag.putByte("Slot", (byte) slot);
        }

        return tag.build();
    }

    public static ItemStack deserializeItem(NbtMap tag) {
        if (!tag.containsKey("Name", NbtType.STRING) || !tag.containsKey("Count", NbtType.BYTE)) {
            return registry.getItem(BlockTypes.AIR);
        }

        return deserializeItem(
                Identifier.fromString(tag.getString("Name")),
                tag.getShort("Damage", (short) 0),
                tag.getByte("Count"),
                tag.getCompound("tag", NbtMap.EMPTY)
        );
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag) {
        CloudItemStackBuilder builder = new CloudItemStackBuilder();

        registry.getSerializer(ItemTypes.byId(id)).deserialize(id, damage, amount, builder, tag);

        return builder.build();
    }

    public static ItemData[] toNetwork(Collection<ItemStack> items) {
        return items.stream().map(item -> ((CloudItemStack) item).getNetworkData()).toArray(ItemData[]::new);
    }

    public static ItemData toNetwork(ItemStack item) {
        int id = registry.getRuntimeId(((CloudItemStack) item).getId());

        NbtMap tag = ((CloudItemStack) item).getNbt();
        short meta;
        if (tag.isEmpty()) {
            tag = null;
            meta = 0;
        } else {
            meta = tag.getShort("Damage", (short) 0);
        }

        String[] canPlace = item.getCanPlaceOn().stream().map(Identifier::toString).toArray(String[]::new);
        String[] canBreak = item.getCanDestroy().stream().map(Identifier::toString).toArray(String[]::new);

        return ItemData.of(id, meta, item.getAmount(), tag, canPlace, canBreak);
    }

    public static ItemStack fromNetwork(ItemData data) {
        Identifier id = registry.getIdentifier(data.getId());
        ItemType type = ItemTypes.byId(id);

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

        CloudItemStackBuilder builder = new CloudItemStackBuilder();
        registry.getSerializer(type).deserialize(id, data.getDamage(), data.getCount(), builder, tag);

        return builder.build();
    }

    public static boolean isNull(ItemStack item) {
        return item == null || item.isNull();
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
            try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(Base64.getDecoder().decode(nbt)))) {
                tag = (NbtMap) stream.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to decode tag", e);
            }
        } else {
            tag = NbtMap.EMPTY;
        }

        Identifier id;
        if (data.containsKey("id")) {
            id = registry.fromLegacy(Utils.toInt(data.get("id")));
        } else {
            id = Identifier.fromString("Name");
        }

        return deserializeItem(id, (short) Utils.toInt(data.getOrDefault("damage", 0)), Utils.toInt(data.getOrDefault("count", 1)), tag);
    }
}
