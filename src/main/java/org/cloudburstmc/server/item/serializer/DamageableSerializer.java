package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Damageable;
import org.cloudburstmc.server.utils.Identifier;

public class DamageableSerializer implements ItemDataSerializer<Damageable> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, Damageable value) {
        tag.putInt("Damage", value.getDurability());
        tag.putBoolean("Unbreakable", value.isUnbreakable());
    }

    @Override
    public Damageable deserialize(Identifier type, Integer meta, NbtMap tag) {
        return Damageable.of(
                tag.getInt("Damage", 0),
                tag.getBoolean("Unbreakable", false)
        );
    }
}
