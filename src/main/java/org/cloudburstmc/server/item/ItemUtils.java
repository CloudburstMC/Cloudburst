package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

        registry.getSerializer(registry.getType(id)).deserialize(id, damage, amount, builder, tag);

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
        ItemType type = registry.getType(id);

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
}
