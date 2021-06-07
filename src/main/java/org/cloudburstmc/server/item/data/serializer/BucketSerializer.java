package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.util.Identifier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketSerializer implements ItemDataSerializer<Bucket> {

    public static final BucketSerializer INSTANCE = new BucketSerializer();

    private static final BiMap<Identifier, Bucket> dataMap = HashBiMap.create(ImmutableMap.<Identifier, Bucket>builder()
            .put(ItemIds.BUCKET, Bucket.EMPTY)
            .put(ItemIds.MILK_BUCKET, Bucket.MILK)
            .put(ItemIds.COD_BUCKET, Bucket.COD)
            .put(ItemIds.SALMON_BUCKET, Bucket.SALMON)
            .put(ItemIds.TROPICAL_FISH_BUCKET, Bucket.TROPICAL_FISH)
            .put(ItemIds.PUFFERFISH_BUCKET, Bucket.PUFFERFISH)
            .put(ItemIds.WATER_BUCKET, Bucket.WATER)
            .put(ItemIds.LAVA_BUCKET, Bucket.LAVA)
            .build());

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, Bucket value) {
        rootTag.putString("Name", dataMap.inverse().getOrDefault(value, ItemIds.BUCKET).toString());
    }

    @Override
    public Bucket deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        return dataMap.getOrDefault(Identifier.fromString(rootTag.getString("Name")), Bucket.EMPTY);
    }
}
