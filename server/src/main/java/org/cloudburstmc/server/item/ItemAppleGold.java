package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemAppleGold extends ItemEdible {

    public ItemAppleGold(Identifier id) {
        super(id);
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return true;
    }
}
