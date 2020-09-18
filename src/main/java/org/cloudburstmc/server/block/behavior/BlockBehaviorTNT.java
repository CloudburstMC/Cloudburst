package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.PrimedTnt;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorTNT extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public boolean canBeActivated(Block block) {
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

    public void prime(Block block) {
        this.prime(block, 80);
    }

    public void prime(Block block, int fuse) {
        prime(block, fuse, null);
    }

    public void prime(Block block, int fuse, Entity source) {
        block.set(BlockStates.AIR, true);

        float mot = ThreadLocalRandom.current().nextFloat() * (float) Math.PI * 2f;

        PrimedTnt primedTnt = EntityRegistry.get().newEntity(EntityTypes.TNT,
                Location.from(block.getPosition().toFloat().add(0.5, 0, 0.5), block.getLevel()));
        primedTnt.setMotion(Vector3f.from(-Math.sin(mot) * 0.02, 0.2, -Math.cos(mot) * 0.02));
        primedTnt.setFuse(fuse);
        primedTnt.setSource(source);
        primedTnt.spawnToAll();

        block.getLevel().addSound(block.getPosition(), Sound.RANDOM_FUSE);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if ((type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) && block.getLevel().isBlockPowered(block.getPosition())) {
            this.prime(block);
        }

        return 0;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (item.getType() == ItemTypes.FLINT_AND_STEEL) {
            item.getBehavior().useOn(item, block);
            this.prime(block, 80, player);
            return true;
        }
        if (item.getType() == ItemTypes.FIREBALL) {
            if (!player.isCreative()) player.getInventory().removeItem(ItemStack.get(ItemTypes.FIREBALL, 0, 1));
            block.getLevel().addSound(player.getPosition(), Sound.MOB_GHAST_FIREBALL);
            this.prime(block, 80, player);
            return true;
        }
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.TNT_BLOCK_COLOR;
    }
}
