package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.potion.CloudPotion;

public class PotionSerializer implements ItemDataSerializer<Potion> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Potion value) {
        rootTag.putShort("Damage", (short) value.getNetworkId());
    }

    @Override
    public Potion deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return new CloudPotion(PotionType.fromLegacy((byte) rootTag.getShort("Damage")), false);
    }
}
