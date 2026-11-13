package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.NoteBlockInstrument;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.EntityBlockHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.event.block.NotePlayEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.cloudburstmc.server.block.util.RedstoneSignals;
import org.cloudburstmc.server.blockentity.NoteBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@UtilityClass
public class NoteBlockHandlers {

    public static final ComplexBlockHandler UPDATE_POWER = block -> {
        NoteBlockEntity music = getOrCreate(block);
        if (music != null) {
            boolean powered = RedstoneSignals.hasNeighborSignal(block);
            boolean risingEdge = powered && !music.isPowered();
            music.setPowered(powered);
            if (risingEdge) {
                music.play();
            }
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> UPDATE_POWER.execute(block);

    public static final UseBlockHandler USE = (block, player, face, item) -> {
        if (player == null || player.isSpectator()) {
            return false;
        }

        if (face == Direction.UP && isHeadItem(block, item)) {
            return false;
        }

        NoteBlockEntity music = getOrCreate(block);
        if (music == null) {
            return false;
        }

        music.setNote((music.getNote() + 1) % 25);
        music.play();

        return true;
    };

    public static final EntityBlockHandler ON_START_MINING = (block, entity) -> {
        NoteBlockEntity music = getOrCreate(block);
        if (music != null) {
            music.play();
        }
    };

    public static NoteBlockInstrument resolveInstrument(Block block) {
        NoteBlockInstrument top = instrumentAt(block.getSide(Direction.UP));
        if (!top.isTunable()) {
            return top;
        }
        NoteBlockInstrument bottom = instrumentAt(block.getSide(Direction.DOWN));
        return bottom.isTunable() ? bottom : NoteBlockInstrument.HARP;
    }

    public static boolean play(Block block, NoteBlockEntity music) {
        NoteBlockInstrument instrument = resolveInstrument(block);
        Block above = block.getSide(Direction.UP);
        if (instrument.isTunable() && (above.getState().getType() != BlockTypes.AIR || !above.getLiquid().isEmpty())) {
            return false;
        }

        NotePlayEvent event = new NotePlayEvent(block, instrument, music.getNote());
        block.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled() || music.isClosed() || !music.isValid() || !(block.getLevel() instanceof CloudLevel level)) {
            return false;
        }

        return emit(level, block, event.getInstrument(), event.getNote());
    }

    private static boolean emit(CloudLevel level, Block block, NoteBlockInstrument instrument, int note) {
        Vector3f position = block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
        int instrumentId = nativeInstrumentId(instrument);
        if (instrumentId >= 0) {
            level.addLevelSoundEvent(position, SoundEvent.NOTE, instrumentId << 8 | note);
        } else {
            SoundType sound = namedSound(instrument);
            if (sound == null) {
                return false;
            }

            float pitch = instrument.isTunable() ? (float) Math.pow(2, (note - 12) / 12.0) : 1;
            level.playSound(position, sound, 3, pitch);
        }

        if (instrument.isTunable()) {
            BlockEventPacket particle = new BlockEventPacket();
            particle.setBlockPosition(block.getPosition());
            particle.setEventType(Math.max(0, instrumentId));
            particle.setEventData(note);
            level.addChunkPacket(block.getPosition(), particle);
        }

        return true;
    }

    private static int nativeInstrumentId(NoteBlockInstrument instrument) {
        return switch (instrument) {
            case HARP -> 0;
            case BASEDRUM -> 1;
            case SNARE -> 2;
            case HAT -> 3;
            case BASS -> 4;
            case FLUTE -> 5;
            case BELL -> 6;
            case GUITAR -> 7;
            case CHIME -> 8;
            case XYLOPHONE -> 9;
            case IRON_XYLOPHONE -> 10;
            case COW_BELL -> 11;
            case DIDGERIDOO -> 12;
            case BIT -> 13;
            case BANJO -> 14;
            case PLING -> 15;
            default -> -1;
        };
    }

    private static @Nullable SoundType namedSound(NoteBlockInstrument instrument) {
        return switch (instrument) {
            case TRUMPET -> SoundTypes.NOTE_TRUMPET;
            case TRUMPET_EXPOSED -> SoundTypes.NOTE_TRUMPET_EXPOSED;
            case TRUMPET_OXIDIZED -> SoundTypes.NOTE_TRUMPET_OXIDIZED;
            case TRUMPET_WEATHERED -> SoundTypes.NOTE_TRUMPET_WEATHERED;
            case ZOMBIE -> SoundTypes.NOTE_ZOMBIE;
            case SKELETON -> SoundTypes.NOTE_SKELETON;
            case CREEPER -> SoundTypes.NOTE_CREEPER;
            case DRAGON -> SoundTypes.NOTE_ENDERDRAGON;
            case WITHER_SKELETON -> SoundTypes.NOTE_WITHERSKELETON;
            case PIGLIN -> SoundTypes.NOTE_PIGLIN;
            default -> null;
        };
    }

    private static NoteBlockInstrument instrumentAt(Block block) {
        return block.requireComponent(BlockComponents.NOTE_BLOCK_INSTRUMENT);
    }

    private static boolean isHeadItem(Block block, ItemStack item) {
        return !item.isEmpty() && CloudItemRegistry.get().requireComponents(item.getType())
                .require(ItemBehaviors.GET_BLOCK).execute(item)
                .map(state -> block.getLevel().getServer().getBlockRegistry()
                        .requireComponent(state.getType(), BlockComponents.NOTE_BLOCK_INSTRUMENT))
                .filter(instrument -> !instrument.isTunable())
                .isPresent();
    }

    private static @Nullable NoteBlockEntity getOrCreate(Block block) {
        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing == null && block.getState().getType() == BlockTypes.NOTE_BLOCK) {
            existing = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.NOTE_BLOCK, block);
        }
        return existing instanceof NoteBlockEntity music && !music.isClosed() ? music : null;
    }
}
