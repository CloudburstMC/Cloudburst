package org.cloudburstmc.server.item.serializer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.SerializedItem;

import java.util.concurrent.TimeUnit;

@Log4j2
public class SerializationCache {

    public static final Cache<ItemStack, SerializedItem> ITEM_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
}
