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
public class CloudScore<T> implements Score<T> {

    protected CloudScoreboardObjective objective;
    protected long id;

    private int amount;
    private String name;

    private ScoreType<T> scoreType;
    private T value;

    private CloudScore() {
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

    public static <U> CloudScoreBuilder<U> providedBuilder(ScoreType<U> scoreType) {
        return new CloudScoreBuilder<>(scoreType);
    }

    public static class CloudScoreBuilder<T> implements ScoreBuilder<T> {

        private final CloudScore<T> score = new CloudScore<>();

        public CloudScoreBuilder(ScoreType<T> scoreType) {
            this.score.scoreType = scoreType;
        }

        @Override
        public CloudScoreBuilder<T> amount(int amount) {
            this.score.amount = amount;
            return this;
        }

        @Override
        public CloudScoreBuilder<T> name(String name) {
            this.score.name = name;
            return this;
        }

        @Override
        public CloudScoreBuilder<T> value(T value) {
            this.score.value = value;
            return this;
        }

        @Override
        public CloudScore<T> build() {
            return this.score;
        }
    }
}
