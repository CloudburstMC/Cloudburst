package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.protocol.bedrock.data.ExperimentData;

import java.util.List;

/**
 * Features required when loading resources and entering the world.
 */
@UtilityClass
public class VanillaExperiments {

    private static final List<ExperimentData> ENABLED = List.of(
            new ExperimentData("data_driven_items", true),
            new ExperimentData("upcoming_creator_features", true),
            new ExperimentData("experimental_molang_features", true)
    );

    public static List<ExperimentData> getEnabled() {
        return ENABLED;
    }
}
