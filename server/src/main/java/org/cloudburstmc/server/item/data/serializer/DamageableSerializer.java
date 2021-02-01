package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.data.Damageable;

public class DamageableSerializer implements ItemDataSerializer<Damageable> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Damageable value) {
        dataTag.putInt("Damage", value.getDurability());
        dataTag.putBoolean("Unbreakable", value.isUnbreakable());
    }

    @Override
    public Damageable deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return Damageable.of(
                dataTag.getInt("Damage", 0),
                dataTag.getBoolean("Unbreakable", false)
        );
    }
}
