package org.cloudburstmc.server.block.serializer.util;

import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import java.util.EnumMap;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.*;

public class TreeSpeciesUtils {

    private static final EnumMap<TreeSpecies, Info> INFO = new EnumMap<>(TreeSpecies.class);
    private static final String[][] NAMES = {
            {"log", TAG_OLD_LOG_TYPE, "leaves", TAG_NEW_LOG_TYPE},
            {"log2", TAG_NEW_LOG_TYPE, "leaves2", TAG_NEW_LEAF_TYPE}
    };

    static {
        TreeSpecies[] values = TreeSpecies.values();

        for (int i = 0; i < values.length; i++) {
            TreeSpecies species = values[i];
            String[] names = NAMES[i & 3];
            INFO.put(species, new Info(names, species.name().toLowerCase()));
        }
    }

    public static Info getInfo(TreeSpecies species) {
        return INFO.get(species);
    }

    @Getter
    public static class Info {
        private final Identifier logType;
        private final Identifier leavesType;
        private final String logStateName;
        private final String leafStateName;
        private final String state;

        private Info(String[] names, String state) {
            this.logType = Identifier.from("minecraft", names[0]);
            this.logStateName = names[1];
            this.leavesType = Identifier.from("minecraft", names[2]);
            this.leafStateName = names[3];
            this.state = state;
        }
    }
}
