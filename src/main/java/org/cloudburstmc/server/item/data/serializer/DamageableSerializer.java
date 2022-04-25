package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.Damageable;
import org.cloudburstmc.api.util.Identifier;

public class DamageableSerializer implements ItemDataSerializer<Damageable> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, Damageable value) {
        tag.putInt("Damage", value.getDurability());
        tag.putBoolean("Unbreakable", value.isUnbreakable());
    }

    @Override
    public Damageable deserialize(Identifier id, NbtMap dataTag) {
        return Damageable.of(
                dataTag.getInt("Damage", 0),
                dataTag.getBoolean("Unbreakable", false)
        );
    }
}
