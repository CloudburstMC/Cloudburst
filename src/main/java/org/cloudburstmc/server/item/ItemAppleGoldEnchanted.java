package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.item in project nukkit.
 */
public class ItemAppleGoldEnchanted extends ItemEdible {

    public ItemAppleGoldEnchanted(Identifier id) {
        super(id);
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return true;
    }
}
