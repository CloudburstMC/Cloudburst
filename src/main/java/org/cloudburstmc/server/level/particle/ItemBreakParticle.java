package org.cloudburstmc.server.level.particle;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class ItemBreakParticle extends GenericParticle {

    public ItemBreakParticle(Vector3f pos, ItemStack item) {
        super(pos, LevelEventType.PARTICLE_ITEM_BREAK, (CloudItemRegistry.get().getRuntimeId(((CloudItemStack) item).getId()) << 16) | ((CloudItemStack) item).getNetworkData().getDamage());
    }
}
