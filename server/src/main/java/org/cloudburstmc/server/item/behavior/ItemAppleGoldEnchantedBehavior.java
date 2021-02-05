package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.item in project nukkit.
 */
public class ItemAppleGoldEnchantedBehavior extends ItemEdibleBehavior {

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, CloudPlayer player) {
        return true;
    }
}
