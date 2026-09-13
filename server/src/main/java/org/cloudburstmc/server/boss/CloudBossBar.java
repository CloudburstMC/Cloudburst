package org.cloudburstmc.server.boss;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.packet.BossEventPacket;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class CloudBossBar implements BossBar {

    private final Set<CloudPlayer> players = new LinkedHashSet<>();
    private Component title;
    private BossBarColor color;
    private BossBarStyle style;
    private double progress = 1;
    private boolean visible = true;

    protected CloudBossBar(Component title, BossBarColor color, BossBarStyle style) {
        this.title = Objects.requireNonNull(title, "title");
        this.color = Objects.requireNonNull(color, "color");
        this.style = Objects.requireNonNull(style, "style");
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public void setTitle(Component title) {
        Objects.requireNonNull(title, "title");
        if (this.title.equals(title)) {
            return;
        }

        this.title = title;
        broadcast(BossEventPacket.Action.UPDATE_NAME);
    }

    @Override
    public BossBarColor getColor() {
        return this.color;
    }

    @Override
    public void setColor(BossBarColor color) {
        Objects.requireNonNull(color, "color");
        if (this.color == color) {
            return;
        }

        this.color = color;
        broadcast(BossEventPacket.Action.UPDATE_STYLE);
    }

    @Override
    public BossBarStyle getStyle() {
        return this.style;
    }

    @Override
    public void setStyle(BossBarStyle style) {
        Objects.requireNonNull(style, "style");
        if (this.style == style) {
            return;
        }

        this.style = style;
        broadcast(BossEventPacket.Action.UPDATE_STYLE);
    }

    @Override
    public double getProgress() {
        return this.progress;
    }

    @Override
    public void setProgress(double progress) {
        Preconditions.checkArgument(Double.isFinite(progress) && progress >= 0 && progress <= 1,
                "progress must be between 0.0 and 1.0");
        if (this.progress == progress) {
            return;
        }

        this.progress = progress;
        broadcast(BossEventPacket.Action.UPDATE_PERCENTAGE);
    }

    @Override
    public void addPlayer(Player player) {
        Preconditions.checkArgument(player instanceof CloudPlayer, "Player is not owned by this server");
        CloudPlayer cloudPlayer = (CloudPlayer) player;
        if (this.players.add(cloudPlayer)) {
            cloudPlayer.trackBossBar(this);
            if (this.visible) {
                show(cloudPlayer);
            }
        }
    }

    @Override
    public void removePlayer(Player player) {
        if (player instanceof CloudPlayer cloudPlayer && this.players.remove(cloudPlayer)) {
            cloudPlayer.untrackBossBar(this);
            if (this.visible) {
                hide(cloudPlayer);
            }
        }
    }

    @Override
    public void removeAll() {
        List.copyOf(this.players).forEach(this::removePlayer);
    }

    @Override
    public List<Player> getPlayers() {
        return List.copyOf(this.players);
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }

        this.visible = visible;
        this.players.forEach(visible ? this::show : this::hide);
    }

    public void refresh(CloudPlayer player) {
        if (this.visible && isBound() && this.players.contains(player)) {
            hide(player);
            show(player);
        }
    }

    public void updatePosition(CloudPlayer player) {
    }

    protected abstract boolean isBound();

    protected abstract long getBossEntityId();

    protected void showBackingEntity(CloudPlayer player) {
    }

    protected void hideBackingEntity(CloudPlayer player) {
    }

    protected final boolean isShownTo(CloudPlayer player) {
        return this.visible && this.players.contains(player);
    }

    protected final void replaceBinding(Runnable replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (this.visible && isBound()) {
            this.players.forEach(this::hide);
        }

        replacement.run();
        if (this.visible && isBound()) {
            this.players.forEach(this::show);
        }
    }

    private void broadcast(BossEventPacket.Action action) {
        if (this.visible && isBound()) {
            this.players.forEach(player -> player.sendPacket(packet(action)));
        }
    }

    private void show(CloudPlayer player) {
        if (isBound()) {
            showBackingEntity(player);
            player.sendPacket(packet(BossEventPacket.Action.CREATE));
        }
    }

    private void hide(CloudPlayer player) {
        if (isBound()) {
            player.sendPacket(packet(BossEventPacket.Action.REMOVE));
            hideBackingEntity(player);
        }
    }

    private BossEventPacket packet(BossEventPacket.Action action) {
        BossEventPacket packet = new BossEventPacket();
        packet.setBossUniqueEntityId(getBossEntityId());
        packet.setAction(action);
        packet.setTitle(BedrockLegacyTextSerializer.getInstance().serialize(this.title));
        packet.setHealthPercentage((float) this.progress);
        packet.setColor(colorId());
        packet.setOverlay(overlayId());
        return packet;
    }

    private int colorId() {
        return switch (this.color) {
            case PINK -> 0;
            case BLUE -> 1;
            case RED -> 2;
            case GREEN -> 3;
            case YELLOW -> 4;
            case PURPLE -> 5;
            case WHITE -> 7;
        };
    }

    private int overlayId() {
        return switch (this.style) {
            case SOLID -> 0;
            case SEGMENTED_6 -> 1;
            case SEGMENTED_10 -> 2;
            case SEGMENTED_12 -> 3;
            case SEGMENTED_20 -> 4;
        };
    }
}
