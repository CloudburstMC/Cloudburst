package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.utils.Identifier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketSerializer implements ItemDataSerializer<Bucket> {

    public static final BucketSerializer INSTANCE = new BucketSerializer();

    private static final BiMap<Short, Bucket> dataMap = HashBiMap.create(ImmutableMap.<Short, Bucket>builder()
            .put((short) 0, Bucket.EMPTY)
            .put((short) 1, Bucket.MILK)
            .put((short) 2, Bucket.COD)
            .put((short) 3, Bucket.SALMON)
            .put((short) 4, Bucket.TROPICAL_FISH)
            .put((short) 5, Bucket.PUFFERFISH)
            .put((short) 8, Bucket.WATER)
            .put((short) 10, Bucket.LAVA)
            .build());

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Bucket value) {
        rootTag.putShort("Damage", dataMap.inverse().getOrDefault(value, (short) 0));
    }

    @Override
    public Bucket deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return dataMap.getOrDefault(rootTag.getShort("Damage"), Bucket.EMPTY);
    }
}
