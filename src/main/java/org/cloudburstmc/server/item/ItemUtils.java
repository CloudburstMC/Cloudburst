package org.cloudburstmc.server.item;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.Identifier;

@UtilityClass
public class ItemUtils {

    public static NbtMap serializeItem(Item item) {
        return serializeItem(item, -1);
    }

    public static NbtMap serializeItem(Item item, int slot) {
        NbtMapBuilder tag = NbtMap.builder()
                .putString("Name", item.getId().toString())
                .putByte("Count", (byte) item.getCount())
                .putShort("Damage", (short) item.getMeta());
        if (slot >= 0) {
            tag.putByte("Slot", (byte) slot);
        }

        NbtMapBuilder nbt = item.getTag().toBuilder();
        item.saveAdditionalData(nbt);
        tag.putCompound("tag", nbt.build());

        return tag.build();
    }

    public static Item deserializeItem(NbtMap tag) {
        if (!(tag.containsKey("Name") || tag.containsKey("id")) && !tag.containsKey("Count")) {
            return Item.get(BlockTypes.AIR);
        }

        Item item;
        try {
            Identifier identifier;
            if (tag.containsKey("Name")) {
                identifier = Identifier.fromString(tag.getString("Name"));
            } else {
                identifier = ItemRegistry.get().fromLegacy(tag.getShort("id"));
            }
            item = Item.get(identifier, !tag.containsKey("Damage") ? 0 : tag.getShort("Damage"), tag.getByte("Count"));
        } catch (Exception e) {
            item = Item.fromString(tag.getString("id"));
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
