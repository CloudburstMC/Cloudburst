package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.PrimedTnt;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

/**
 * Created on 2015/12/8 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorTNT extends BlockBehaviorSolid {

    public BlockBehaviorTNT(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public int getBurnChance() {
        return 15;
    }

    @Override
    public int getBurnAbility() {
        return 100;
    }

    public void prime() {
        this.prime(80);
    }

    public void prime(int fuse) {
        prime(fuse, null);
    }

    public void prime(int fuse, Entity source) {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(AIR), true);
        float mot = ThreadLocalRandom.current().nextFloat() * (float) Math.PI * 2f;

        PrimedTnt primedTnt = EntityRegistry.get().newEntity(EntityTypes.TNT,
                Location.from(this.getPosition().toFloat().add(0.5, 0, 0.5), this.getLevel()));
        primedTnt.setMotion(Vector3f.from(-Math.sin(mot) * 0.02, 0.2, -Math.cos(mot) * 0.02));
        primedTnt.setFuse(fuse);
        primedTnt.setSource(source);
        primedTnt.spawnToAll();
        this.level.addSound(this.getPosition(), Sound.RANDOM_FUSE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if ((type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) && this.level.isBlockPowered(this.getPosition())) {
            this.prime();
        }

        return 0;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.FLINT_AND_STEEL) {
            item.useOn(this);
            this.prime(80, player);
            return true;
        }
        if (item.getId() == ItemIds.FIREBALL) {
            if (!player.isCreative()) player.getInventory().removeItem(Item.get(ItemIds.FIREBALL, 0, 1));
            this.level.addSound(player.getPosition(), Sound.MOB_GHAST_FIREBALL);
            this.prime(80, player);
            return true;
        }
        return false;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.TNT_BLOCK_COLOR;
    }
}
