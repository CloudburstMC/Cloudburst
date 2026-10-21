package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.event.block.CauldronChangeReason;
import org.cloudburstmc.api.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.api.event.player.PlayerBucketEvent;
import org.cloudburstmc.api.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.particle.ParticleEmitterTypes;
import org.cloudburstmc.api.level.particle.ParticleNumber;
import org.cloudburstmc.api.level.particle.ParticleStruct;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.Bucket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.item.VanillaItemDyes;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.server.block.component.CauldronBlockHandlers.*;

@UtilityClass
public class CauldronItemInteractions {

    private static final int FULL = 6;
    private static final int BOTTLE = 2;
    private static final int DYE_COST = 2;

    private static final Map<ItemType, Bucket> BUCKETS = Map.of(
            ItemTypes.WATER_BUCKET, Bucket.WATER,
            ItemTypes.LAVA_BUCKET, Bucket.LAVA,
            ItemTypes.POWDER_SNOW_BUCKET, Bucket.POWDER_SNOW);

    private static final Map<ItemType, PotionForm> POTIONS = Map.of(
            ItemTypes.POTION, PotionForm.DRINKABLE,
            ItemTypes.SPLASH_POTION, PotionForm.SPLASH,
            ItemTypes.LINGERING_POTION, PotionForm.LINGERING);

    private static final Set<ItemType> DYED_SHULKERS = Set.of(
            ItemTypes.WHITE_SHULKER_BOX, ItemTypes.ORANGE_SHULKER_BOX, ItemTypes.MAGENTA_SHULKER_BOX,
            ItemTypes.LIGHT_BLUE_SHULKER_BOX, ItemTypes.YELLOW_SHULKER_BOX, ItemTypes.LIME_SHULKER_BOX,
            ItemTypes.PINK_SHULKER_BOX, ItemTypes.GRAY_SHULKER_BOX, ItemTypes.LIGHT_GRAY_SHULKER_BOX,
            ItemTypes.CYAN_SHULKER_BOX, ItemTypes.PURPLE_SHULKER_BOX, ItemTypes.BLUE_SHULKER_BOX,
            ItemTypes.BROWN_SHULKER_BOX, ItemTypes.GREEN_SHULKER_BOX, ItemTypes.RED_SHULKER_BOX,
            ItemTypes.BLACK_SHULKER_BOX);

    public static boolean use(Block block, Player player, Direction face, ItemStack item) {
        Bucket incoming = BUCKETS.get(item.getType());
        if (incoming != null) {
            emptyBucket(block, player, face, item, incoming);
            return true;
        }

        if (item.getType() == ItemTypes.BUCKET) {
            return fillBucket(block, player, face, item);
        }

        if (item.getType() == ItemTypes.GLASS_BOTTLE) {
            return fillBottle(block, player, item);
        }

        PotionForm form = POTIONS.get(item.getType());
        if (form != null) {
            return emptyBottle(block, player, item, form);
        }

        if (fill(block) == 0 || content(block) != Bucket.WATER) {
            return false;
        }

        CauldronContents stored = contents(block);
        if (stored.potion() != null) {
            return tipArrows(block, player, item, stored);
        }

        Integer dye = VanillaItemDyes.color(item.getType());
        if (dye != null) {
            int mixed = stored.color() == null ? dye : VanillaItemDyes.mix(stored.color(), dye);
            exchange(block, player, item, ItemStack.EMPTY, 1, block.getState(),
                    new CauldronContents(null, PotionForm.DRINKABLE, mixed),
                    CauldronChangeReason.DYE_WATER, SoundTypes.CAULDRON_ADDDYE, ExchangeMode.CONTAINER);
            return true;
        }

        if (VanillaItemDyes.isDyeable(item.getType().getId())) {
            return dyeEquipment(block, player, item, stored);
        }

        return stored.color() == null && wash(block, player, item);
    }

    private static void emptyBucket(Block block, Player player, Direction face, ItemStack item, Bucket incoming) {
        boolean submerged = block.getRelative(0, 1, 0).getLiquid().getType().isSameFamily(LiquidTypes.WATER);
        if (incoming != Bucket.WATER && submerged) {
            return;
        }

        Bucket current = content(block);
        boolean mixed = fill(block) > 0 && (contents(block).potion() != null
                || incoming == Bucket.LAVA && current != Bucket.LAVA
                || incoming != Bucket.LAVA && current == Bucket.LAVA);
        SoundType sound = mixed ? SoundTypes.CAULDRON_EXPLODE : switch (incoming) {
            case LAVA -> SoundTypes.BUCKET_EMPTY_LAVA;
            case POWDER_SNOW -> SoundTypes.BUCKET_EMPTY_POWDER_SNOW;
            default -> SoundTypes.CAULDRON_FILLWATER;
        };

        ItemStack output = bucketResult(block, player, face, item, ItemStack.from(ItemTypes.BUCKET), true);
        if (output == null) {
            return;
        }

        BlockState updated = state(mixed ? Bucket.WATER : incoming, mixed ? 0 : FULL);
        exchange(block, player, item, output, 1, updated, CauldronContents.PLAIN, CauldronChangeReason.BUCKET_EMPTY, sound, ExchangeMode.CONTAINER);
    }

    private static boolean fillBucket(Block block, Player player, Direction face, ItemStack item) {
        if (fill(block) != FULL || !contents(block).equals(CauldronContents.PLAIN)) {
            return false;
        }

        Bucket current = content(block);
        ItemType filled = switch (current) {
            case LAVA -> ItemTypes.LAVA_BUCKET;
            case POWDER_SNOW -> ItemTypes.POWDER_SNOW_BUCKET;
            default -> ItemTypes.WATER_BUCKET;
        };

        SoundType sound = switch (current) {
            case LAVA -> SoundTypes.BUCKET_FILL_LAVA;
            case POWDER_SNOW -> SoundTypes.BUCKET_FILL_POWDER_SNOW;
            default -> SoundTypes.CAULDRON_TAKEWATER;
        };

        ItemStack output = bucketResult(block, player, face, item, ItemStack.from(filled), false);
        if (output == null) {
            return true;
        }

        exchange(block, player, item, output, 1, state(Bucket.WATER, 0), CauldronContents.PLAIN, CauldronChangeReason.BUCKET_FILL, sound, ExchangeMode.CONTAINER);
        return true;
    }

    private static @Nullable ItemStack bucketResult(Block block, Player player, Direction face, ItemStack item, ItemStack output, boolean empty) {
        CauldronContents previous = contents(block);
        PlayerBucketEvent event = empty ? new PlayerBucketEmptyEvent(player, block, block, face, item, output) : new PlayerBucketFillEvent(player, block, block, face, item, output);
        block.getLevel().getServer().getEventManager().fire(event);

        if (event.isCancelled() || block.getLevel().getBlockState(block.getPosition()) != block.getState() || !previous.equals(contents(block))) {
            return null;
        }

        return event.getItemStack();
    }

    private static boolean fillBottle(Block block, Player player, ItemStack item) {
        if (fill(block) < BOTTLE || content(block) != Bucket.WATER) {
            return false;
        }

        CauldronContents stored = contents(block);
        PotionType potion = stored.potion() == null ? PotionTypes.WATER : stored.potion();
        ItemType type = potionItem(stored.form());
        ItemStack output = ItemStack.builder(type)
                .setData(ItemDataComponents.POTION_TYPE, potion)
                .build();
        BlockState updated = state(Bucket.WATER, fill(block) - BOTTLE);
        SoundType sound = stored.potion() == null ? SoundTypes.CAULDRON_TAKEWATER : SoundTypes.CAULDRON_TAKEPOTION;

        exchange(block, player, item, output, 1, updated, stored, CauldronChangeReason.BOTTLE_FILL, sound, ExchangeMode.CONTAINER);
        return true;
    }

    private static boolean emptyBottle(Block block, Player player, ItemStack item, PotionForm form) {
        int level = fill(block);
        if (level > 0 && content(block) != Bucket.WATER) {
            return false;
        }

        PotionType potion = item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER);
        CauldronContents incoming = potion == PotionTypes.WATER && form == PotionForm.DRINKABLE ? CauldronContents.PLAIN : new CauldronContents(potion, form, null);
        CauldronContents stored = contents(block);
        boolean mixed = level > 0 && (stored.potion() != incoming.potion() || stored.form() != incoming.form());
        if (!mixed && level == FULL) {
            return false;
        }

        int updatedLevel = mixed ? 0 : Math.min(FULL, level + BOTTLE);
        BlockState updated = state(Bucket.WATER, updatedLevel);
        CauldronContents updatedContents = mixed ? CauldronContents.PLAIN : incoming;
        SoundType sound;
        if (mixed) {
            sound = SoundTypes.CAULDRON_EXPLODE;
        } else {
            sound = incoming.potion() == null ? SoundTypes.CAULDRON_FILLWATER : SoundTypes.CAULDRON_FILLPOTION;
        }

        exchange(block, player, item, ItemStack.from(ItemTypes.GLASS_BOTTLE), 1, updated, updatedContents, CauldronChangeReason.BOTTLE_EMPTY, sound, ExchangeMode.CONTAINER);
        return true;
    }

    private static boolean dyeEquipment(Block block, Player player, ItemStack item, CauldronContents stored) {
        Integer color = stored.color();
        if (color == null && item.get(ItemDataComponents.DYED_COLOR) == null) {
            return false;
        }

        ItemStackBuilder builder = item.toBuilder().amount(1);
        CauldronChangeReason reason;
        SoundType sound;
        int cost;
        if (color == null) {
            builder.removeData(ItemDataComponents.DYED_COLOR);
            reason = CauldronChangeReason.ARMOR_WASH;
            sound = SoundTypes.CAULDRON_CLEANARMOR;
            cost = 1;
        } else {
            builder.setData(ItemDataComponents.DYED_COLOR, color);
            reason = CauldronChangeReason.ARMOR_DYE;
            sound = SoundTypes.CAULDRON_DYEARMOR;
            cost = DYE_COST;
        }

        BlockState updated = state(Bucket.WATER, Math.max(0, fill(block) - cost));
        exchange(block, player, item, builder.build(), 1, updated, stored, reason, sound, ExchangeMode.TRANSFORM);
        return true;
    }

    private static boolean wash(Block block, Player player, ItemStack item) {
        BannerData banner = item.get(ItemDataComponents.BANNER_DATA);
        if (banner != null && !banner.getPatterns().isEmpty()) {
            BannerData washed = BannerData.of(banner.getType(), banner.getBase(), banner.getPatterns().subList(0, banner.getPatterns().size() - 1));
            ItemStack output = item.toBuilder()
                    .amount(1)
                    .setData(ItemDataComponents.BANNER_DATA, washed)
                    .build();
            BlockState updated = state(Bucket.WATER, fill(block) - 1);

            exchange(block, player, item, output, 1, updated, CauldronContents.PLAIN, CauldronChangeReason.BANNER_WASH, SoundTypes.CAULDRON_CLEANBANNER, ExchangeMode.COPY);
            return true;
        }

        if (DYED_SHULKERS.contains(item.getType())) {
            ItemStack output = item.toBuilder()
                    .itemType(ItemTypes.UNDYED_SHULKER_BOX)
                    .amount(1)
                    .removeData(ItemDataComponents.BLOCK_STATE)
                    .removeData(ItemDataComponents.COLOR)
                    .build();
            BlockState updated = state(Bucket.WATER, fill(block) - 1);

            exchange(block, player, item, output, 1, updated, CauldronContents.PLAIN, CauldronChangeReason.SHULKER_WASH, null, ExchangeMode.COPY);
            return true;
        }

        return false;
    }

    private static boolean tipArrows(Block block, Player player, ItemStack item, CauldronContents stored) {
        int level = fill(block);
        if (item.getType() != ItemTypes.ARROW || item.get(ItemDataComponents.POTION_TYPE) != null || level < BOTTLE) {
            return false;
        }

        int capacity = level == FULL ? 64 : level / BOTTLE * 16;
        int count = Math.min(item.getCount(), capacity);
        int cost = Math.min(level, (count + 15) / 16 * BOTTLE);
        ItemStack output = item.toBuilder()
                .amount(count)
                .setData(ItemDataComponents.POTION_TYPE, stored.potion())
                .build();
        BlockState updated = state(Bucket.WATER, level - cost);

        exchange(block, player, item, output, count, updated, stored, CauldronChangeReason.ARROW_TIP, SoundTypes.CAULDRON_TAKEPOTION, ExchangeMode.CONTAINER);
        return true;
    }

    private static void exchange(Block block, Player player, ItemStack input, ItemStack output, int count,
                                 BlockState updated, CauldronContents contents, CauldronChangeReason reason,
                                 @Nullable SoundType sound, ExchangeMode mode) {
        if (!player.getInventory().getSelectedItem().equals(input)
                || !change(block, player, reason, updated, contents)) {
            return;
        }

        if (player.isCreative() && mode != ExchangeMode.TRANSFORM) {
            if (!output.isEmpty() && (mode == ExchangeMode.COPY || !player.getInventory().contains(output))) {
                give(player, output);
            }
        } else if (input.getCount() == count) {
            player.getInventory().setSelectedItem(output);
        } else {
            player.getInventory().setSelectedItem(input.toBuilder().amount(input.getCount() - count).build());
            give(player, output);
        }

        if (sound != null) {
            playEffect(block, sound, contents);
        }
    }

    private static void playEffect(Block block, SoundType sound, CauldronContents previous) {
        Block refreshed = block.refresh();
        CauldronContents current = contents(refreshed);
        CauldronContents colored = current.equals(CauldronContents.PLAIN) ? previous : current;
        int color = 0;
        if (colored.color() != null) {
            color = colored.color();
        }

        int fill = refreshed.getState().getType() == BlockTypes.CAULDRON ? fill(refreshed) : 0;
        Vector3f position = block.getPosition().toFloat().add(0.5f, (6 + 1.5f * fill) / 16f, 0.5f);

        if (block.getLevel() instanceof CloudLevel level) {
            level.addChunkPacket(position, LevelEffectPacketFactory.cauldronInteraction(position, sound, color));
        } else {
            block.getLevel().playSound(position, sound);
        }

        if (sound == SoundTypes.CAULDRON_FILLPOTION && current.potion() != null) {
            block.getLevel().spawnParticleEffect(potionParticles(current.potion()), position);
        }
    }

    private static ParticleEmitter potionParticles(PotionType potion) {
        int color = CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF);
        ParticleStruct tint = new ParticleStruct(Map.of(
                "r", new ParticleNumber((color >> 16 & 0xFF) / 255f),
                "g", new ParticleNumber((color >> 8 & 0xFF) / 255f),
                "b", new ParticleNumber((color & 0xFF) / 255f),
                "a", new ParticleNumber(1)));

        return new ParticleEmitter(ParticleEmitterTypes.CAULDRON_SPELL_EMITTER, null, Map.of(
                "variable.particlecount", new ParticleNumber(10),
                "variable.color", tint));
    }

    private static void give(Player player, ItemStack item) {
        if (item.isEmpty()) {
            return;
        }

        for (ItemStack remaining : player.getInventory().addItem(item)) {
            player.getLevel().dropItem(player.getPosition(), remaining);
        }
    }

    private static ItemType potionItem(PotionForm form) {
        return switch (form) {
            case DRINKABLE -> ItemTypes.POTION;
            case SPLASH -> ItemTypes.SPLASH_POTION;
            case LINGERING -> ItemTypes.LINGERING_POTION;
        };
    }

    private enum ExchangeMode {
        CONTAINER,
        TRANSFORM,
        COPY
    }
}
