package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

public class ItemHoneyBottle extends ItemEdible {
    public ItemHoneyBottle(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return true;
    }
}
