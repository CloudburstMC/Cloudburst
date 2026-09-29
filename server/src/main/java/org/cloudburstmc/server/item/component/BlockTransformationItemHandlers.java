package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class BlockTransformationItemHandlers {

    public static UseOnHandler useOn(Map<BlockType, BlockTransformation> rules) {
        return (item, entity, position, face, click) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            CloudLevel level = player.getLevel();
            Block block = level.getBlock(position);
            BlockTransformation rule = rules.get(block.getState().getType());
            if (rule == null) {
                return item;
            }

            boolean airAbove = level.getBlockState(position.add(0, 1, 0)).getType() == BlockTypes.AIR;
            BlockState replacement = rule.apply(block.getState(), face, airAbove);
            if (replacement == null) {
                return item;
            }

            List<BlockChange> changes = relatedChanges(level, block, replacement);
            for (BlockChange change : changes) {
                EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, change.block(), change.newState());
                level.getServer().getEventManager().fire(event);
                if (event.isCancelled()) {
                    return item;
                }
            }

            for (BlockChange change : changes) {
                if (level.isOutsideBuildHeight(change.block().getPosition().getY())
                        || !level.getBlockState(change.block().getPosition()).equals(change.block().getState())) {
                    return item;
                }
            }

            boolean stillAirAbove = level.getBlockState(position.add(0, 1, 0)).getType() == BlockTypes.AIR;
            if (!replacement.equals(rule.apply(block.getState(), face, stillAirAbove))) {
                return item;
            }

            level.batchBlockUpdates(() -> {
                for (BlockChange change : changes) {
                    level.setBlockState(change.block().getPosition(), change.newState());
                }
            });

            playEffects(level, rule, changes);
            if (rule.drop() != null && level.getGameRules().get(GameRules.DO_TILE_DROPS)) {
                Vector3f dropPosition = position.toFloat().add(0.5f, 0.5f, 0.5f)
                        .add(face.relative(Vector3i.ZERO).toFloat().mul(0.65f));
                level.dropItem(dropPosition, ItemStack.from(rule.drop()));
            }

            if (player.isCreative()) {
                return item;
            }

            return rule.consumesItem() ? item.decreaseCount()
                    : CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE).execute(item, 1, player);
        };
    }

    private static List<BlockChange> relatedChanges(CloudLevel level, Block block, BlockState replacement) {
        List<BlockChange> changes = new ArrayList<>();
        changes.add(new BlockChange(block, replacement));

        if (block.getState().getTraits().containsKey(BlockTraits.IS_UPPER_BLOCK)) {
            Vector3i partnerPosition = block.getPosition().add(0,
                    block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK) ? -1 : 1, 0);
            Block partner = level.getBlock(partnerPosition);
            if (partner.getState().getType() == block.getState().getType() && partner.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)
                    != block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
                changes.add(new BlockChange(partner, replacement.getType().getDefaultState().copyTraits(partner.getState())));
            }
        } else if (level.getLoadedBlockEntity(block.getPosition()) instanceof Chest chest && chest.isPaired()) {
            Chest partner = chest.getPair();
            if (partner != null && partner.getBlockState().getType() == block.getState().getType()) {
                changes.add(new BlockChange(level.getBlock(partner.getPosition()), replacement.getType().getDefaultState().copyTraits(partner.getBlockState())));
            }
        }

        return changes;
    }

    private static void playEffects(CloudLevel level, BlockTransformation rule, List<BlockChange> changes) {
        BlockChange clicked = changes.getFirst();
        int soundData = rule.sound() == SoundEvent.ITEM_USE_ON ? CloudBlockRegistry.REGISTRY.getRuntimeId(clicked.newState()) : -1;
        level.addLevelSoundEvent(clicked.block().getPosition(), rule.sound(), soundData);
        for (BlockChange change : changes) {
            Vector3f center = change.block().getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
            if (rule.particle() != null) {
                level.addLevelEvent(center, rule.particle(), 0);
            }

            if (rule.sound() == SoundEvent.EXTINGUISH_FIRE) {
                for (int i = 0; i < 10; i++) {
                    level.spawnParticle(ParticleTypes.SMOKE, center);
                }
            }
        }
    }
}
