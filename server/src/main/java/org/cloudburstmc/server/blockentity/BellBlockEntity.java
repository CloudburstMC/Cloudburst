package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.Bell;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.block.BellRingEvent;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class BellBlockEntity extends BaseBlockEntity implements Bell {

    private static final int SHAKE_DURATION = 50;

    private int ringTick;
    private @Nullable Direction strikeDirection;
    private boolean firingRingEvent;

    public BellBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean ring(@Nullable Entity entity, @Nullable Direction direction) {
        if (this.isClosed() || !this.isValid() || this.firingRingEvent) {
            return false;
        }

        Direction strike = direction == null ? this.getBlockState().ensureTrait(BlockTraits.DIRECTION) : direction;
        if (!strike.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Bell strike direction must be horizontal");
        }

        BellRingEvent event = new BellRingEvent(this.getBlock(), strike, entity);
        this.firingRingEvent = true;
        try {
            this.getServer().getEventManager().fire(event);
        } finally {
            this.firingRingEvent = false;
        }

        if (event.isCancelled() || this.isClosed() || !this.isValid()) {
            return false;
        }

        this.ringTick = this.getServer().getTick();
        this.strikeDirection = strike;
        this.getLevel().playSound(this.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), SoundTypes.BLOCK_BELL_HIT);
        this.spawnToAll();
        return true;
    }

    @Override
    public boolean isShaking() {
        return this.strikeDirection != null && this.elapsedTicks() < SHAKE_DURATION;
    }

    @Override
    public int getShakingTicks() {
        return this.isShaking() ? this.elapsedTicks() : 0;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.remove("Direction");
        tag.remove("Ringing");
        tag.remove("Ticks");
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        Direction strike = this.strikeDirection;
        boolean shaking = strike != null && this.isShaking();
        tag.putInt("Direction", strike == null ? 0 : strike.getHorizontalIndex());
        tag.putBoolean("Ringing", shaking);
        tag.putInt("Ticks", shaking ? this.elapsedTicks() : 0);
    }

    private int elapsedTicks() {
        return this.getServer().getTick() - this.ringTick;
    }
}
