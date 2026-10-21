package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Cauldron;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.block.CauldronChangeReason;
import org.cloudburstmc.api.event.block.CauldronLevelChangeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.data.Bucket;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class CauldronBlockHandlers {

    private static final int FULL = 6;
    private static final int BOTTLE = 2;

    public static final UseBlockHandler USE = (block, player, face, item) ->
            player != null && !player.isSpectator() && CauldronItemInteractions.use(block, player, face, item);

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        Cauldron cauldron;
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof Cauldron existing) {
            cauldron = existing;
        } else {
            cauldron = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.CAULDRON, block);
            cauldron.setDirty();
            cauldron.spawnToAll();
        }

        if (fill(block) == 0 || content(block) != Bucket.WATER) {
            cauldron.setContents(CauldronContents.PLAIN);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        Block tip = PointedDripstoneBlockHandlers.findTipAbove(block);
        if (tip == null) {
            return;
        }

        LiquidType fluid = PointedDripstoneBlockHandlers.fillingFluid(tip);
        if (fluid == null || !canReceiveDrip(block, fluid)) {
            return;
        }

        boolean lava = fluid == LiquidTypes.LAVA;
        Bucket content = lava ? Bucket.LAVA : Bucket.WATER;
        int updatedLevel = lava ? FULL : Math.min(FULL, fill(block) + BOTTLE);
        BlockState updated = state(content, updatedLevel);

        if (change(block, null, CauldronChangeReason.NATURAL_FILL, updated)) {
            SoundType sound = lava ? SoundTypes.CAULDRON_DRIP_LAVA_POINTED_DRIPSTONE : SoundTypes.CAULDRON_DRIP_WATER_POINTED_DRIPSTONE;
            RandomGenerator source = random == null ? ThreadLocalRandom.current() : random;
            float pitch = 0.9f + source.nextFloat() * 0.1f;
            block.getLevel().playSound(block.getPosition(), sound, 2f, pitch);
        }
    };

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        int fill = fill(block);
        BoundingBox bounds = entity.getBoundingBox();
        float surface = block.getY() + (6 + 1.5f * fill) / 16f;
        if (fill == 0 || bounds.getMinY() >= surface
                || bounds.getMaxY() <= block.getY() + 0.25f
                || bounds.getMaxX() <= block.getX() + 0.125f
                || bounds.getMinX() >= block.getX() + 0.875f
                || bounds.getMaxZ() <= block.getZ() + 0.125f
                || bounds.getMinZ() >= block.getZ() + 0.875f) {
            return;
        }

        if (content(block) == Bucket.LAVA) {
            DefaultBlockHandlers.LAVA_ENTITY_INSIDE.execute(block, entity, precise);
            return;
        }

        if (!entity.isOnFire() || !mayChangeContents(block, entity)) {
            return;
        }

        BlockState updated = state(Bucket.WATER, Math.max(0, fill - BOTTLE));
        if (change(block, entity, CauldronChangeReason.EXTINGUISH, updated)) {
            entity.extinguish();
        }
    };

    public static boolean canReceiveDrip(Block block, LiquidType fluid) {
        return (fluid == LiquidTypes.WATER || fluid == LiquidTypes.LAVA)
                && block.getState().getType() == BlockTypes.CAULDRON && fill(block) < FULL && isPlain(block)
                && (fill(block) == 0 || content(block) == Bucket.WATER && fluid == LiquidTypes.WATER);
    }

    public static void collectPrecipitation(Block block, boolean snow, RandomGenerator random) {
        if (block.getState().getType() != BlockTypes.CAULDRON) {
            return;
        }

        int fill = fill(block);
        Bucket precipitation = snow ? Bucket.POWDER_SNOW : Bucket.WATER;
        if (fill == FULL || !isPlain(block) || fill > 0 && content(block) != precipitation || random.nextFloat() >= (snow ? 0.1f : 0.05f)) {
            return;
        }

        BlockState updated = state(precipitation, Math.min(FULL, fill + BOTTLE));
        change(block, null, CauldronChangeReason.NATURAL_FILL, updated);
    }

    private static boolean change(Block block, @Nullable Entity entity, CauldronChangeReason reason, BlockState updated) {
        return change(block, entity, reason, updated, contents(block));
    }

    public static boolean change(Block block, @Nullable Entity entity, CauldronChangeReason reason, BlockState updated, CauldronContents contents) {
        CauldronContents previous = contents(block);
        ItemStack held = entity instanceof Player player ? player.getInventory().getSelectedItem() : null;
        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(block, entity, reason, updated, contents);
        block.getLevel().getServer().getEventManager().fire(event);

        if (event.isCancelled() || block.getLevel().getBlockState(block.getPosition()) != block.getState() || !previous.equals(contents(block))
                || entity instanceof Player player && !Objects.equals(held, player.getInventory().getSelectedItem())) {
            return false;
        }

        BlockState result = event.getNewState();
        CauldronContents accepted = event.getNewContents();
        if (result.getType() != BlockTypes.CAULDRON || result.ensureTrait(BlockTraits.FILL_LEVEL) == 0 || result.ensureTrait(BlockTraits.CAULDRON_TYPE) != Bucket.WATER) {
            accepted = CauldronContents.PLAIN;
        }

        if (result != block.getState() && !block.getLevel().setBlockState(block.getPosition(), result)) {
            return false;
        }

        if (block.getLevel().getBlockState(block.getPosition()) != result) {
            return false;
        }

        if (result.getType() == BlockTypes.CAULDRON) {
            Cauldron cauldron = block.getLevel().getBlockEntity(block.getPosition()) instanceof Cauldron existing ? existing : null;
            if (cauldron == null && !accepted.equals(CauldronContents.PLAIN)) {
                cauldron = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.CAULDRON, block.refresh());
            }

            if (cauldron != null) {
                cauldron.setContents(accepted);
            }
        }

        return true;
    }

    public static CauldronContents contents(Block block) {
        return block.getLevel().getBlockEntity(block.getPosition()) instanceof Cauldron cauldron ? cauldron.getContents() : CauldronContents.PLAIN;
    }

    private static boolean isPlain(Block block) {
        return contents(block).equals(CauldronContents.PLAIN);
    }

    private static boolean mayChangeContents(Block block, Entity entity) {
        if (entity instanceof Player player) {
            return !player.isSpectator() && (player.isOp() || !(block.getLevel() instanceof CloudLevel level) || !level.isInSpawnRadius(block.getPosition()));
        }

        return block.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public static int fill(Block block) {
        return block.getState().ensureTrait(BlockTraits.FILL_LEVEL);
    }

    public static Bucket content(Block block) {
        return block.getState().ensureTrait(BlockTraits.CAULDRON_TYPE);
    }

    public static BlockState state(Bucket content, int fill) {
        return BlockTypes.CAULDRON.getDefaultState()
                .withTrait(BlockTraits.CAULDRON_TYPE, content)
                .withTrait(BlockTraits.FILL_LEVEL, fill);
    }
}
