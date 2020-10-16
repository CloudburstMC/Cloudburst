package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Noteblock;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.IdentityHashMap;
import java.util.Map;

public class BlockBehaviorNoteblock extends BlockBehaviorSolid {

    private static final Map<BlockType, Instrument> INSTRUMENTS = new IdentityHashMap<>();

    static {
        INSTRUMENTS.put(BlockTypes.GOLD_BLOCK, Instrument.GLOCKENSPIEL);
        INSTRUMENTS.put(BlockTypes.CLAY, Instrument.FLUTE);
        INSTRUMENTS.put(BlockTypes.PACKED_ICE, Instrument.CHIME);
        INSTRUMENTS.put(BlockTypes.WOOL, Instrument.GUITAR);
        INSTRUMENTS.put(BlockTypes.BONE_BLOCK, Instrument.XYLOPHONE);
        INSTRUMENTS.put(BlockTypes.IRON_BLOCK, Instrument.VIBRAPHONE);
        INSTRUMENTS.put(BlockTypes.SOUL_SAND, Instrument.COW_BELL);
        INSTRUMENTS.put(BlockTypes.PUMPKIN, Instrument.DIDGERIDOO);
        INSTRUMENTS.put(BlockTypes.EMERALD_BLOCK, Instrument.SQUARE_WAVE);
        INSTRUMENTS.put(BlockTypes.HAY_BLOCK, Instrument.BANJO);
        INSTRUMENTS.put(BlockTypes.GLOWSTONE, Instrument.ELECTRIC_PIANO);
        INSTRUMENTS.put(BlockTypes.LOG, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.PLANKS, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_SLAB, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_FENCE, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_PRESSURE_PLATE, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WOODEN_TRAPDOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.STANDING_SIGN, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WALL_SIGN, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.NOTEBLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.BOOKSHELF, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.CHEST, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.TRAPPED_CHEST, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.CRAFTING_TABLE, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.JUKEBOX, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.BROWN_MUSHROOM_BLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.RED_MUSHROOM_BLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.DAYLIGHT_DETECTOR, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.STANDING_BANNER, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.WALL_BANNER, Instrument.BASS);
        INSTRUMENTS.put(BlockTypes.SAND, Instrument.DRUM);
        INSTRUMENTS.put(BlockTypes.GRAVEL, Instrument.DRUM);
        INSTRUMENTS.put(BlockTypes.CONCRETE_POWDER, Instrument.DRUM);
        INSTRUMENTS.put(BlockTypes.GLASS, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.GLASS_PANE, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.STAINED_GLASS_PANE, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.STAINED_GLASS, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.BEACON, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.SEA_LANTERN, Instrument.STICKS);
        INSTRUMENTS.put(BlockTypes.STONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.SANDSTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.RED_SANDSTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.COBBLESTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.MOSSY_COBBLESTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.BRICK_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONEBRICK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.NETHER_BRICK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.QUARTZ_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONE_SLAB, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONE_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONE_WALL, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.NETHER_BRICK_FENCE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.BEDROCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.GOLD_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.IRON_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.COAL_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.LAPIS_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.DIAMOND_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.REDSTONE_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.EMERALD_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.DROPPER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.DISPENSER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.FURNACE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.OBSIDIAN, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.GLOWING_OBSIDIAN, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.MOB_SPAWNER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONE_PRESSURE_PLATE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.NETHERRACK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.QUARTZ_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.ENCHANTING_TABLE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.END_PORTAL_FRAME, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.END_STONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.END_BRICKS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.ENDER_CHEST, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STAINED_HARDENED_CLAY, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.HARDENED_CLAY, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.PRISMARINE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.COAL_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.PURPUR_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.MAGMA, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.CONCRETE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.STONECUTTER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockTypes.OBSERVER, Instrument.BASS_DRUM);
    }


    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item) && this.createBlockEntity(block) != null;
    }

    public int getStrength(Block block) {
        Noteblock blockEntity = this.getBlockEntity(block);
        return blockEntity != null ? blockEntity.getNote() : 0;
    }

    public void increaseStrength(Block block) {
        Noteblock blockEntity = this.getBlockEntity(block);
        if (blockEntity != null) {
            blockEntity.changeNote();
        }
    }

    public Instrument getInstrument(Block block) {
        return INSTRUMENTS.getOrDefault(block.down().getState().getType(), Instrument.PIANO);
    }

    public void emitSound(Block block) {
        if (block.up().getState().getType() != BlockTypes.AIR) return;

        Instrument instrument = this.getInstrument(block);

        val level = block.getLevel();
        level.addLevelSoundEvent(block.getPosition(), SoundEvent.NOTE, instrument.ordinal() << 8 | this.getStrength(block));

        BlockEventPacket pk = new BlockEventPacket();
        pk.setBlockPosition(block.getPosition());
        pk.setEventType(instrument.ordinal());
        pk.setEventData(this.getStrength(block));
        level.addChunkPacket(block.getPosition(), pk);
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        this.increaseStrength(block);
        this.emitSound(block);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            Noteblock blockEntity = this.getBlockEntity(block);
            if (blockEntity != null) {
                if (block.getLevel().isBlockPowered(block.getPosition())) {
                    if (!blockEntity.isPowered()) {
                        this.emitSound(block);
                    }
                    blockEntity.setPowered(true);
                } else {
                    blockEntity.setPowered(false);
                }
            }
        }
        return super.onUpdate(block, type);
    }

    private Noteblock getBlockEntity(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (blockEntity instanceof Noteblock) {
            return (Noteblock) blockEntity;
        }
        return null;
    }

    private Noteblock createBlockEntity(Block block) {
        return BlockEntityRegistry.get().newEntity(BlockEntityTypes.NOTEBLOCK, block);
    }

    public enum Instrument {
        PIANO(Sound.NOTE_HARP),
        BASS_DRUM(Sound.NOTE_BD),
        DRUM(Sound.NOTE_SNARE),
        STICKS(Sound.NOTE_HAT),
        BASS(Sound.NOTE_BASS),
        GLOCKENSPIEL(Sound.NOTE_BELL),
        FLUTE(Sound.NOTE_FLUTE),
        CHIME(Sound.NOTE_CHIME),
        GUITAR(Sound.NOTE_GUITAR),
        XYLOPHONE(Sound.NOTE_XYLOPHONE),
        VIBRAPHONE(Sound.NOTE_IRON_XYLOPHONE),
        COW_BELL(Sound.NOTE_COW_BELL),
        DIDGERIDOO(Sound.NOTE_DIDGERIDOO),
        SQUARE_WAVE(Sound.NOTE_BIT),
        BANJO(Sound.NOTE_BANJO),
        ELECTRIC_PIANO(Sound.NOTE_PLING);

        private final Sound sound;

        Instrument(Sound sound) {
            this.sound = sound;
        }

        public Sound getSound() {
            return sound;
        }
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
