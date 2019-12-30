package cn.nukkit.block;

import cn.nukkit.utils.Identifier;

/**
 * Created by Pub4Game on 21.02.2016.
 */
public class BlockSlime extends BlockSolid {

    public BlockSlime(Identifier id) {
        super(id);
    }

    @Override
    public double getHardness() {
        return 0;
    }

    @Override
    public double getResistance() {
        return 0;
    }
}
