package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.DamageItemHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FishingRodItemHandlers {
    public static final UseHandler USE = FishingRodItemHandlers::use;

    private static ItemStack use(ItemStack rod, Entity entity) {
        if (!(entity instanceof CloudPlayer player)) {
            return rod;
        }

        int damage = player.useFishingRod(rod);
        if (damage == 0 || player.isCreative()) {
            return rod;
        }

        DamageItemHandler handler = CloudItemRegistry.get().requireComponent(rod.getType(), ItemComponents.ON_DAMAGE);
        return handler == null ? rod : handler.execute(rod, damage, player);
    }
}
