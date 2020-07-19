package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemIds.DYE;

/**
 * Created on 2015/11/23 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorFlower extends FloodableBlockBehavior {
    public static final int TYPE_POPPY = 0;
    public static final int TYPE_BLUE_ORCHID = 1;
    public static final int TYPE_ALLIUM = 2;
    public static final int TYPE_AZURE_BLUET = 3;
    public static final int TYPE_RED_TULIP = 4;
    public static final int TYPE_ORANGE_TULIP = 5;
    public static final int TYPE_WHITE_TULIP = 6;
    public static final int TYPE_PINK_TULIP = 7;
    public static final int TYPE_OXEYE_DAISY = 8;

    public BlockBehaviorFlower(Identifier id) {
        super(id);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down.getId() == GRASS || down.getId() == DIRT || down.getId() == FARMLAND || down.getId() == PODZOL) {
            this.getLevel().setBlock(blockState.getPosition(), this, true);

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().isTransparent()) {
                this.getLevel().useBreakOn(this.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.getId() == DYE && item.getMeta() == 0x0f) { //Bone meal
            if (player != null && player.getGamemode().isSurvival()) {
                item.decrementCount();
            }

            this.level.addParticle(new BoneMealParticle(this.getPosition()));

            for (int i = 0; i < 8; i++) {
                Vector3i vec = this.getPosition().add(
                        ThreadLocalRandom.current().nextInt(-3, 4),
                        ThreadLocalRandom.current().nextInt(-1, 2),
                        ThreadLocalRandom.current().nextInt(-3, 4));

                if (level.getBlock(vec).getId() == AIR && level.getBlock(vec.down()).getId() == GRASS && vec.getY() >= 0 && vec.getY() < 256) {
                    if (ThreadLocalRandom.current().nextInt(10) == 0) {
                        this.level.setBlock(vec, this.getUncommonFlower(), true);
                    } else {
                        this.level.setBlock(vec, BlockState.get(this.getId()), true);
                    }
                }
            }

            return true;
        }

        return false;
    }

    protected BlockState getUncommonFlower() {
        return BlockState.get(YELLOW_FLOWER);
    }
}
