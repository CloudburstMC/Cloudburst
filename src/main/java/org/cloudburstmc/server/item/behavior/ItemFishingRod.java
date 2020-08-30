package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.item in project nukkit.
 */
public class ItemFishingRod extends ItemTool {

    public ItemFishingRod(Identifier id) {
        super(id);
    }

    @Override
    public int getEnchantAbility() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        if (player.fishing != null) {
            player.stopFishing(true);
        } else {
            player.startFishing(this);
            this.setMeta(this.getMeta() + 1);
        }
        return true;
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_FISHING_ROD;
    }
}
