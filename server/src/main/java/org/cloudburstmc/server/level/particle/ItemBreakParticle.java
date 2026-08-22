package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public final class ItemBreakParticle extends GenericParticle {

    public ItemBreakParticle(Vector3f pos, ItemStack item) {
        super(pos, ParticleType.ICON_CRACK, data(item));
    }

    private static int data(ItemStack item) {
        int runtimeId = CloudItemRegistry.get().getDefinition(item.getType().getId()).getRuntimeId();
        int damage = ItemUtils.toNetwork(item).getDamage();
        return (runtimeId << 16) | damage;
    }
}
