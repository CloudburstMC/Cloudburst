package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Jukebox;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.JukeboxSong;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.particle.ParticleEmission;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.sound.StopSoundData;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundUpdateSoundDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.cloudburstmc.protocol.bedrock.packet.RecordStartedPacket;
import org.cloudburstmc.server.blockentity.data.BlockEntityData;
import org.cloudburstmc.server.blockentity.data.JukeboxData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.network.SoundPacketFactory;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class JukeboxBlockEntity extends BaseBlockEntity implements Jukebox, AutomatedBlockStorage {

    private static final AtomicLong SOUND_HANDLES = new AtomicLong();
    private static final int TICKS_PER_SECOND = 20;
    private static final int AUDIBLE_DISTANCE_SQUARED = 64 * 64;

    private final Set<CloudPlayer> listeners = new HashSet<>();
    private @Nullable JukeboxSong playingSong;
    private int playbackTicks;
    private long soundHandle;

    public JukeboxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return JukeboxData.class;
    }

    @Override
    protected JukeboxData data() {
        return (JukeboxData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        this.clearPlayback();
        ItemStack record = this.getRecord();
        this.data().setRecordItem(record.isEmpty() ? null : record.withCount(1));

        int elapsed = tag.getInt("PlaybackTicks", -1);
        JukeboxSong song = songFor(record);
        if (song != null && elapsed >= 0 && elapsed < song.durationTicks()) {
            this.beginPlayback(song, elapsed);
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        if (this.playingSong != null) {
            tag.putInt("PlaybackTicks", this.playbackTicks);
        } else {
            tag.remove("PlaybackTicks");
        }
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        ItemStack record = this.getRecord();
        if (record.isEmpty()) {
            tag.remove("RecordItem");
        } else {
            tag.putCompound("RecordItem", ItemStackNbtCodec.serializeItem(record));
        }
    }

    @Override
    public SlotGroupType<Jukebox> getSlotGroupType() {
        return SlotGroupTypes.JUKEBOX;
    }

    @Override
    public Jukebox getBlockEntity() {
        return this;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        Objects.checkIndex(slot, this.size());
        return this.getRecord();
    }

    @Override
    public int getMaxStackSize(int slot) {
        Objects.checkIndex(slot, 1);
        return 1;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        Objects.checkIndex(slot, 1);
        this.setRecord(item);
    }

    @Override
    public boolean canInsertFrom(int slot, Direction side, ItemStack item) {
        return slot == 0 && this.getRecord().isEmpty() && songFor(item) != null;
    }

    @Override
    public boolean canExtractBelow(int slot) {
        return slot == 0 && !this.isPlaying();
    }

    @Override
    public ItemStack getRecord() {
        return Objects.requireNonNullElse(this.data().getRecordItem(), ItemStack.EMPTY);
    }

    @Override
    public void setRecord(ItemStack record) {
        Objects.requireNonNull(record, "record");
        JukeboxSong song = songFor(record);
        if (!record.isEmpty() && song == null) {
            throw new IllegalArgumentException("Item has no jukebox song");
        }

        ItemStack stored = record.isEmpty() ? ItemStack.EMPTY : record.withCount(1);
        if (this.isClosed() || stored.equals(this.getRecord())) {
            return;
        }

        this.clearPlayback();
        this.data().setRecordItem(stored.isEmpty() ? null : stored);
        if (song != null) {
            this.beginPlayback(song, 0);
        }
        this.setDirty();
        this.spawnToAll();
        this.updateSignals();
        this.getLevel().updateComparatorOutputLevel(this.getPosition());
    }

    @Override
    public int getComparatorOutput() {
        JukeboxSong song = songFor(this.getRecord());
        return song == null ? 0 : song.comparatorOutput();
    }

    @Override
    public boolean eject() {
        ItemStack record = this.getRecord();
        if (this.isClosed() || record.isEmpty()) {
            return false;
        }

        this.setRecord(ItemStack.EMPTY);
        this.getLevel().dropItem(this.getPosition().toFloat().add(0.5f, 1.1f, 0.5f), record);
        return true;
    }

    @Override
    public boolean isPlaying() {
        return !this.isClosed() && this.playingSong != null;
    }

    @Override
    public boolean startPlaying() {
        JukeboxSong song = songFor(this.getRecord());
        if (this.isClosed() || song == null || this.isPlaying()) {
            return false;
        }
        this.beginPlayback(song, 0);
        this.setDirty();
        this.updateSignals();
        return true;
    }

    @Override
    public void stopPlaying() {
        if (this.isPlaying()) {
            this.clearPlayback();
            this.setDirty();
            this.updateSignals();
        }
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();

        JukeboxSong song = this.playingSong;
        if (song == null || this.isClosed()) {
            return false;
        }

        if (this.playbackTicks >= song.durationTicks()) {
            this.stopPlaying();
            return false;
        }

        if (this.playbackTicks % TICKS_PER_SECOND == 0) {
            this.updateListeners(song);
            this.getLevel().spawnParticle(new ParticleEmission(ParticleTypes.NOTE), this.center().add(0, 0.7f, 0));
            this.setDirty();
        }

        this.playbackTicks++;
        return true;
    }

    @Override
    public void onBreak() {
        this.clearPlayback();
        this.dropItemOnBreak(this.getRecord());
        this.data().setRecordItem(null);
    }

    @Override
    public void close() {
        this.stopListeners();
        super.close();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean isMovable() {
        return false;
    }

    public static @Nullable JukeboxSong songFor(ItemStack item) {
        return item.isEmpty() ? null : CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.JUKEBOX_PLAYABLE);
    }

    private void beginPlayback(JukeboxSong song, int elapsed) {
        this.playingSong = song;
        this.playbackTicks = elapsed;
        this.soundHandle = SOUND_HANDLES.incrementAndGet();
        this.scheduleUpdate();
    }

    private void clearPlayback() {
        this.stopListeners();
        this.playingSong = null;
        this.playbackTicks = 0;
    }

    private void stopListeners() {
        if (!this.listeners.isEmpty()) {
            ClientboundUpdateSoundDataPacket stop = this.stopPacket();
            for (CloudPlayer player : this.listeners) {
                if (player.isConnected()) {
                    player.sendPacket(stop);
                }
            }
            this.listeners.clear();
        }
    }

    private void updateListeners(JukeboxSong song) {
        Vector3f center = this.center();
        Iterator<CloudPlayer> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            CloudPlayer player = iterator.next();
            if (!this.canHear(player, center)) {
                if (player.isConnected()) {
                    player.sendPacket(this.stopPacket());
                }
                iterator.remove();
            }
        }

        for (CloudPlayer player : this.getLevel().getPlayers().values()) {
            if (this.canHear(player, center) && this.listeners.add(player)) {
                SoundPlayback playback = new SoundPlayback(song.sound(), 4, 1, 0, false,
                        this.playbackTicks / (float) TICKS_PER_SECOND);
                PlaySoundPacket play = SoundPacketFactory.play(playback, center);
                play.setServerSoundHandle(this.soundHandle);
                player.sendPacket(play);

                RecordStartedPacket started = new RecordStartedPacket();
                started.setBlockPos(this.getPosition());
                started.setServerSoundHandle(this.soundHandle);
                player.sendPacket(started);
            }
        }
    }

    private boolean canHear(CloudPlayer player, Vector3f center) {
        return player.isConnected() && player.isSpawned() && player.getLevel() == this.getLevel()
                && player.getPosition().distanceSquared(center) <= AUDIBLE_DISTANCE_SQUARED
                && player.isChunkSent(this.getPosition().getX() >> 4, this.getPosition().getZ() >> 4);
    }

    private ClientboundUpdateSoundDataPacket stopPacket() {
        ClientboundUpdateSoundDataPacket packet = new ClientboundUpdateSoundDataPacket();
        packet.setServerSoundHandle(this.soundHandle);
        packet.setEvent(new StopSoundData());
        return packet;
    }

    private Vector3f center() {
        return this.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
    }

    private void updateSignals() {
        this.getLevel().updateAroundRedstone(this.getPosition(), null);
    }
}
