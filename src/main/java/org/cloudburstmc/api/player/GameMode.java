package org.cloudburstmc.api.player;

import com.google.common.collect.Sets;
import lombok.Getter;

import java.util.*;

@Getter
public class GameMode {

    private static final Map<String, GameMode> nameMap = new HashMap<>();

    public static final GameMode SURVIVAL = GameMode.builder(0, "survival", "s")
            .set(AdventureSetting.BUILD, true)
            .set(AdventureSetting.MINE, true)
            .set(AdventureSetting.WORLD_BUILDER, true)
            .survival().register().build();
    public static final GameMode CREATIVE = GameMode.builder(1, "creative", "c")
            .set(AdventureSetting.BUILD, true)
            .set(AdventureSetting.MINE, true)
            .set(AdventureSetting.WORLD_BUILDER, true)
            .set(AdventureSetting.ALLOW_FLIGHT, true)
            .register().build();
    public static final GameMode ADVENTURE = GameMode.builder(2, "adventure", "a")
            .set(AdventureSetting.WORLD_IMMUTABLE, true)
            .set(AdventureSetting.BUILD, false)
            .set(AdventureSetting.MINE, false)
            .register().survival().visitor().build();
    public static final GameMode SPECTATOR = GameMode.builder(4, "spectator", "spc")
            .set(AdventureSetting.WORLD_IMMUTABLE, true)
            .set(AdventureSetting.ALLOW_FLIGHT, true)
            .set(AdventureSetting.FLYING, true)
            .set(AdventureSetting.NO_CLIP, true)
            .register().visitor().build();

    private final int vanillaId;
    private final String name;
    private final boolean visitor;
    private final boolean survival;
    private final Map<AdventureSetting, Boolean> adventureSettings;
    private final Set<String> aliases;

    private GameMode(int vanillaId, String name, boolean visitor, boolean survival, Map<AdventureSetting, Boolean> adventureSettings, Set<String> aliases) {
        this.vanillaId = vanillaId;
        this.name = name;
        this.visitor = visitor;
        this.survival = survival;
        this.adventureSettings = adventureSettings;
        this.aliases = aliases;
    }

    public String getTranslation() {
        return "%gameMode." + name;
    }

    public static GameMode from(String name) {
        return nameMap.get(name);
    }

    public static GameMode from(int id) {
        switch (id & 0x03) {
            case 0:
                return SURVIVAL;
            case 1:
                return CREATIVE;
            case 2:
                return ADVENTURE;
            default:
                return SPECTATOR;
        }
    }

    public static Builder builder(int vanillaId, String name, String... aliases) {
        return new Builder(vanillaId, name, aliases);
    }

    public static class Builder {

        private final int vanillaId;
        private final String name;
        private final Set<String> aliases;

        private boolean visitor = false;
        private boolean survival = false;
        private boolean register = false;
        private final Map<AdventureSetting, Boolean> adventureSettings = new EnumMap<>(AdventureSetting.class);

        {
            for (AdventureSetting type : AdventureSetting.values()) {
                adventureSettings.put(type, type.getDefaultValue());
            }
        }

        private Builder(int vanillaId, String name, String... aliases) {
            this.vanillaId = vanillaId;
            this.name = name;
            this.aliases = Sets.newHashSet(aliases);
            this.aliases.add(Integer.toString(vanillaId));
        }

        public Builder visitor() {
            visitor = true;
            return this;
        }

        public Builder survival() {
            survival = true;
            return this;
        }

        public Builder set(AdventureSetting type, boolean value) {
            adventureSettings.put(type, value);
            return this;
        }

        public Builder register() {
            register = true;
            return this;
        }

        public GameMode build() {
            GameMode gm = new GameMode(vanillaId, name, visitor, survival, Collections.unmodifiableMap(adventureSettings), aliases);

            if (register) {
                nameMap.put(name, gm);
                for (String alias : gm.aliases) {
                    nameMap.put(alias, gm);
                }
            }

            return gm;
        }
    }
}
