package org.cloudburstmc.server.scoreboard.impl;

import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.scoreboard.DisplayMode;
import org.cloudburstmc.server.scoreboard.Score;
import org.cloudburstmc.server.scoreboard.ScoreType;
import org.cloudburstmc.server.scoreboard.ScoreboardCriteria;
import org.cloudburstmc.server.scoreboard.ScoreboardObjective;
import org.cloudburstmc.server.scoreboard.SortOrder;
import com.nukkitx.protocol.bedrock.data.ScoreInfo;
import com.nukkitx.protocol.bedrock.packet.RemoveObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetDisplayObjectivePacket;
import com.nukkitx.protocol.bedrock.packet.SetScorePacket;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class CloudScoreboardObjective implements ScoreboardObjective {
    public static final int MAX_NAME_LENGTH = 16;
    public static final int MAX_DISPLAY_NAME_LENGTH = 32;

    protected CloudScoreboard scoreboard;
    protected AtomicLong id = new AtomicLong(0);

    private String name;
    private DisplayMode displayMode;
    private SortOrder sortOrder = SortOrder.DESCENDING;

    private String displayName;
    private ScoreboardCriteria criteria = ScoreboardCriteria.DUMMY;

    protected final Map<String, Score<?>> scores = new HashMap<>();

    private CloudScoreboardObjective() {
    }

    @Override
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        this.refresh();
    }

    @Override
    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;

        this.refresh();
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;

        this.refresh();
    }

    @Override
    public Score<?> getScore(String name) {
        return this.scores.get(name);
    }

    @Override
    public <T> Score<T> getScore(String name, ScoreType<T> scoreType) {
        Score<?> score = this.scores.get(name);
        if (score == null) {
            return null;
        }
        if (!scoreType.getSupportedType().isAssignableFrom(score.getValue().getClass())) {
            return null;
        }
        return (Score<T>) score;
    }

    @Override
    public <T> Score<T> getOrCreateScore(String name, ScoreType<T> type, T value, int amount) {
        Score<?> score = this.scores.get(name);
        if (score != null) {
            if (!value.getClass().isAssignableFrom(score.getValue().getClass())) {
                throw new IllegalArgumentException("Score " + name + " for type " + score.getValue().getClass() + " is not an instance of " + value.getClass() + "!");
            }
            return (Score<T>) this.scores.get(name);
        }
        score = Score.builder(type).value(value).name(name).amount(amount).build();
        createScore(score);
        return (Score<T>) score;
    }

    @Override
    public void createScore(Score<?> score) {
        this.scores.put(score.getName(), score);

        CloudScore<?> scoreImpl = (CloudScore<?>) score;
        scoreImpl.objective = this;
        scoreImpl.id = this.id.getAndIncrement();

        SetScorePacket setScorePacket = new SetScorePacket();
        setScorePacket.setAction(SetScorePacket.Action.SET);
        setScorePacket.setInfos(Collections.singletonList(scoreImpl.createScoreInfo()));
        Server.broadcastPacket(this.scoreboard.getPlayers(), setScorePacket);
    }

    @Override
    public void removeScore(String name) {
        CloudScore<?> scoreImpl = (CloudScore<?>) this.scores.remove(name);
        if (scoreImpl == null) {
            return;
        }

        SetScorePacket setScorePacket = new SetScorePacket();
        setScorePacket.setAction(SetScorePacket.Action.REMOVE);
        setScorePacket.setInfos(Collections.singletonList(scoreImpl.createScoreInfo()));
        Server.broadcastPacket(this.scoreboard.getPlayers(), setScorePacket);
    }

    protected void refresh() {
        // We need to remove the objective so we can resend it
        RemoveObjectivePacket removeObjectivePacket = new RemoveObjectivePacket();
        removeObjectivePacket.setObjectiveId(this.getName());
        Server.broadcastPacket(this.scoreboard.getPlayers(), removeObjectivePacket);

        SetDisplayObjectivePacket displayObjectivePacket = new SetDisplayObjectivePacket();
        displayObjectivePacket.setObjectiveId(this.name);
        displayObjectivePacket.setCriteria(this.criteria.name().toLowerCase());
        displayObjectivePacket.setDisplayName(this.displayName);
        displayObjectivePacket.setDisplaySlot(this.displayMode.name().toLowerCase());
        displayObjectivePacket.setSortOrder(this.sortOrder.ordinal());
        Server.broadcastPacket(this.scoreboard.getPlayers(), displayObjectivePacket);

        List<ScoreInfo> scoreInfos = new ArrayList<>();
        for (Score<?> score : this.scores.values()) {
            scoreInfos.add(((CloudScore<?>) score).createScoreInfo());
        }

        SetScorePacket setScorePacket = new SetScorePacket();
        setScorePacket.setAction(SetScorePacket.Action.SET);
        setScorePacket.setInfos(scoreInfos);
        Server.broadcastPacket(this.scoreboard.getPlayers(), setScorePacket);
    }

    public static ScoreboardObjectiveBuilder providedBuilder() {
        return new CloudScoreboardObjectiveBuilder();
    }

    public static class CloudScoreboardObjectiveBuilder implements ScoreboardObjectiveBuilder {

        private final CloudScoreboardObjective objective = new CloudScoreboardObjective();

        @Override
        public ScoreboardObjectiveBuilder name(String name) {
            this.objective.name = name;
            return this;
        }

        @Override
        public ScoreboardObjectiveBuilder displayMode(DisplayMode displayMode) {
            this.objective.displayMode = displayMode;
            return this;
        }

        @Override
        public ScoreboardObjectiveBuilder sortOrder(SortOrder sortOrder) {
            this.objective.sortOrder = sortOrder;
            return this;
        }

        @Override
        public ScoreboardObjectiveBuilder displayName(String displayName) {
            this.objective.displayName = displayName;
            return this;
        }

        @Override
        public ScoreboardObjectiveBuilder criteria(ScoreboardCriteria criteria) {
            this.objective.criteria = criteria;
            return this;
        }

        @Override
        public ScoreboardObjectiveBuilder scores(Score<?>... scores) {
            for (Score<?> score : scores) {
                this.objective.scores.put(score.getName(), score);
            }
            return this;
        }

        @Override
        public ScoreboardObjective build() {
            return this.objective;
        }
    }
}
