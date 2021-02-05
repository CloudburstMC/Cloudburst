package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemAppleGoldBehavior extends ItemEdibleBehavior {

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, CloudPlayer player) {
        return true;
    }
}
