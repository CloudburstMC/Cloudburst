package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

@UtilityClass
@SuppressWarnings({"rawtypes", "unchecked"})
public class ItemUtils {

    public static NbtMap serializeItem(ItemStack item) {
        return serializeItem(item, -1);
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMapBuilder tag = NbtMap.builder()
                .putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getAmount())
                .putShort("Damage", (short) 0);

        if (item.getType().getMetadataClass() != null) {
            ItemDataSerializer serializer = CloudItemRegistry.get().getSerializer(item.getType());
            serializer.serialize(item, tag, item.getMetadata(item.getType().getMetadataClass()));
        }

        if (slot >= 0) {
            tag.putByte("Slot", (byte) slot);
        }

        NbtMapBuilder nbt = item.getTag().toBuilder();
        item.saveAdditionalData(nbt);
        tag.putCompound("tag", nbt.build());

        return tag.build();
    }

    public static ItemStack deserializeItem(NbtMap tag) {
        if (!(tag.containsKey("Name") || tag.containsKey("id")) && !tag.containsKey("Count")) {
            return ItemStack.get(BlockIds.AIR);
        }

        ItemStack item;
        try {
            Identifier identifier;
            if (tag.containsKey("Name")) {
                identifier = Identifier.fromString(tag.getString("Name"));
            } else {
                identifier = CloudItemRegistry.get().fromLegacy(tag.getShort("id"));
            }
            item = ItemStack.get(identifier, !tag.containsKey("Damage") ? 0 : tag.getShort("Damage"), tag.getByte("Count"));
        } catch (Exception e) {
            item = ItemStack.fromString(tag.getString("id"));
            item.setMeta(!tag.containsKey("Damage") ? 0 : tag.getShort("Damage"));
            item.setCount(tag.getByte("Count"));
        }

        NbtMap tagTag = tag.getCompound("tag", NbtMap.EMPTY);
        if (tagTag != NbtMap.EMPTY) {
            item.loadAdditionalData(tagTag);
        }

        return item;
    }
}
