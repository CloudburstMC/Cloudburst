package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.BlockEventPacket;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Noteblock;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

public class BlockBehaviorNoteblock extends BlockBehaviorSolid {

    private static final Map<Identifier, Instrument> INSTRUMENTS = new IdentityHashMap<>();

    static {
        INSTRUMENTS.put(BlockIds.GOLD_BLOCK, Instrument.GLOCKENSPIEL);
        INSTRUMENTS.put(BlockIds.CLAY, Instrument.FLUTE);
        INSTRUMENTS.put(BlockIds.PACKED_ICE, Instrument.CHIME);
        INSTRUMENTS.put(BlockIds.WOOL, Instrument.GUITAR);
        INSTRUMENTS.put(BlockIds.BONE_BLOCK, Instrument.XYLOPHONE);
        INSTRUMENTS.put(BlockIds.IRON_BLOCK, Instrument.VIBRAPHONE);
        INSTRUMENTS.put(BlockIds.SOUL_SAND, Instrument.COW_BELL);
        INSTRUMENTS.put(BlockIds.PUMPKIN, Instrument.DIDGERIDOO);
        INSTRUMENTS.put(BlockIds.EMERALD_BLOCK, Instrument.SQUARE_WAVE);
        INSTRUMENTS.put(BlockIds.HAY_BLOCK, Instrument.BANJO);
        INSTRUMENTS.put(BlockIds.GLOWSTONE, Instrument.ELECTRIC_PIANO);
        INSTRUMENTS.put(BlockIds.LOG, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.LOG2, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.PLANKS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DOUBLE_WOODEN_SLAB, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.WOODEN_SLAB, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.OAK_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.SPRUCE_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.BIRCH_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.JUNGLE_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.ACACIA_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DARK_OAK_STAIRS, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.FENCE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.SPRUCE_FENCE_GATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.BIRCH_FENCE_GATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.JUNGLE_FENCE_GATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DARK_OAK_FENCE_GATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.ACACIA_FENCE_GATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.WOODEN_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.SPRUCE_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.BIRCH_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.JUNGLE_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.ACACIA_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DARK_OAK_DOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.WOODEN_PRESSURE_PLATE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.TRAPDOOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.STANDING_SIGN, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.WALL_SIGN, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.NOTEBLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.BOOKSHELF, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.CHEST, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.TRAPPED_CHEST, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.CRAFTING_TABLE, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.JUKEBOX, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.BROWN_MUSHROOM_BLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.RED_MUSHROOM_BLOCK, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DAYLIGHT_DETECTOR, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.DAYLIGHT_DETECTOR_INVERTED, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.STANDING_BANNER, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.WALL_BANNER, Instrument.BASS);
        INSTRUMENTS.put(BlockIds.SAND, Instrument.DRUM);
        INSTRUMENTS.put(BlockIds.GRAVEL, Instrument.DRUM);
        INSTRUMENTS.put(BlockIds.CONCRETE_POWDER, Instrument.DRUM);
        INSTRUMENTS.put(BlockIds.GLASS, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.GLASS_PANE, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.STAINED_GLASS_PANE, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.STAINED_GLASS, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.BEACON, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.SEA_LANTERN, Instrument.STICKS);
        INSTRUMENTS.put(BlockIds.STONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.SANDSTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.RED_SANDSTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.COBBLESTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.MOSSY_COBBLESTONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.BRICK_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONEBRICK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.NETHER_BRICK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.RED_NETHER_BRICK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.QUARTZ_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.DOUBLE_STONE_SLAB, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONE_SLAB, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.DOUBLE_STONE_SLAB2, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONE_SLAB2, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONE_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.BRICK_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONE_BRICK_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.NETHER_BRICK_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.SANDSTONE_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.QUARTZ_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.RED_SANDSTONE_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.PURPUR_STAIRS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.COBBLESTONE_WALL, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.NETHER_BRICK_FENCE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.BEDROCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.GOLD_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.IRON_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.COAL_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.LAPIS_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.DIAMOND_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.REDSTONE_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.LIT_REDSTONE_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.EMERALD_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.DROPPER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.DISPENSER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.FURNACE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.LIT_FURNACE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.OBSIDIAN, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.GLOWING_OBSIDIAN, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.MOB_SPAWNER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONE_PRESSURE_PLATE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.NETHERRACK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.QUARTZ_ORE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.ENCHANTING_TABLE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.END_PORTAL_FRAME, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.END_STONE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.END_BRICKS, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.ENDER_CHEST, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STAINED_HARDENED_CLAY, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.HARDENED_CLAY, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.PRISMARINE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.COAL_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.PURPUR_BLOCK, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.MAGMA, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.CONCRETE, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.STONECUTTER, Instrument.BASS_DRUM);
        INSTRUMENTS.put(BlockIds.OBSERVER, Instrument.BASS_DRUM);
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

    @Override
    public float getHardness() {
        return 0.8f;
    }

    @Override
    public float getResistance() {
        return 4f;
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
        if (block.up().getState().getType() != BlockIds.AIR) return;

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
