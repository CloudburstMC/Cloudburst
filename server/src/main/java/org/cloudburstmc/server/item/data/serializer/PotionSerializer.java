package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.potion.Potion;
import org.cloudburstmc.server.utils.Identifier;

public class PotionSerializer implements ItemDataSerializer<Potion> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Potion value) {
        rootTag.putShort("Damage", (short) value.getId());
    }

    @Override
    public Potion deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return Potion.getPotion(rootTag.getShort("Damage"));
    }
}
