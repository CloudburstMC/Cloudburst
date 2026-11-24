package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BucketEntityDataPersistenceTest {

    @Test
    void sulfurCubeBucketPreservesBabyStateAndHealth() {
        PersistenceFixtures.ensureInitialized();
        BucketEntityData data = new BucketEntityData(3f, false, false, true);
        ItemStack bucket = ItemStack.builder(ItemTypes.SULFUR_CUBE_BUCKET)
                .setData(ItemDataComponents.BUCKET_ENTITY_DATA, data)
                .build();

        ItemStack reloaded = ItemStackNbtCodec.deserializeItem(ItemStackNbtCodec.serializeItem(bucket));

        assertEquals(data, reloaded.get(ItemDataComponents.BUCKET_ENTITY_DATA));
    }
}
