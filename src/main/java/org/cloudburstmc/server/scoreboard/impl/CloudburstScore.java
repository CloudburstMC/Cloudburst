package org.cloudburstmc.server.scoreboard.impl;

import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.scoreboard.Score;
import org.cloudburstmc.server.scoreboard.ScoreType;
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;

import java.util.Collections;

@Getter
public class CloudburstScore<T> implements Score<T> {

    protected CloudburstScoreboardObjective objective;
    protected long id;

    private int amount;
    private String name;

    private ScoreType<T> scoreType;
    private T value;

    private CloudburstScore() {
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;

        ScoreInfo scoreInfo = this.createScoreInfo();
        SetScorePacket setScorePacket = new SetScorePacket();
        setScorePacket.setInfos(Collections.singletonList(scoreInfo));

        setScorePacket.setAction(SetScorePacket.Action.REMOVE);
        Server.broadcastPacket(this.objective.scoreboard.getPlayers(), setScorePacket);

        setScorePacket = new SetScorePacket();
        setScorePacket.setAction(SetScorePacket.Action.SET);
        setScorePacket.setInfos(Collections.singletonList(scoreInfo));
        Server.broadcastPacket(this.objective.scoreboard.getPlayers(), setScorePacket);
    }

    @Override
    public void setValue(T value) {
        this.value = value;

        ScoreInfo scoreInfo = this.createScoreInfo();
        SetScorePacket setScorePacket = new SetScorePacket();
        setScorePacket.setInfos(Collections.singletonList(scoreInfo));

        setScorePacket.setAction(SetScorePacket.Action.REMOVE);
        Server.broadcastPacket(this.objective.scoreboard.getPlayers(), setScorePacket);

        setScorePacket = new SetScorePacket();
        setScorePacket.setAction(SetScorePacket.Action.SET);
        setScorePacket.setInfos(Collections.singletonList(scoreInfo));
        Server.broadcastPacket(this.objective.scoreboard.getPlayers(), setScorePacket);
    }

    protected ScoreInfo createScoreInfo() {
        if (this.scoreType == ScoreType.PLAYER || this.scoreType == ScoreType.ENTITY) {
            return new ScoreInfo(
                    this.id,
                    this.objective.getName(),
                    this.amount,
                    ScoreInfo.ScorerType.valueOf(this.scoreType.toString().toUpperCase()),
                    ((Entity) this.value).getUniqueId()
            );
        }
        return new ScoreInfo(
                this.id,
                this.objective.getName(),
                this.amount,
                (String) this.value
        );
    }

    public static <U> NukkitScoreBuilder<U> providedBuilder(ScoreType<U> scoreType) {
        return new NukkitScoreBuilder<>(scoreType);
    }

    public static class NukkitScoreBuilder<T> implements ScoreBuilder<T> {

        private final CloudburstScore<T> score = new CloudburstScore<>();

        public NukkitScoreBuilder(ScoreType<T> scoreType) {
            this.score.scoreType = scoreType;
        }

        @Override
        public NukkitScoreBuilder<T> amount(int amount) {
            this.score.amount = amount;
            return this;
        }

        @Override
        public NukkitScoreBuilder<T> name(String name) {
            this.score.name = name;
            return this;
        }

        @Override
        public NukkitScoreBuilder<T> value(T value) {
            this.score.value = value;
            return this;
        }

        @Override
        public CloudburstScore<T> build() {
            return this.score;
        }
    }
}
