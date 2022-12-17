package org.cloudburstmc.server.level.particle;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class ItemBreakParticle extends GenericParticle {

    public ItemBreakParticle(Vector3f pos, ItemStack item) {
        super(pos, ParticleType.ICON_CRACK, (CloudItemRegistry.get().getDefinition(item.getType().getId()).getRuntimeId() << 16) | ItemUtils.toNetwork(item).getDamage());
    }
}
