package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicksPerConfig {

    @Builder.Default
    private int autosave = 6000;

    @Builder.Default
    private int animalSpawns = 400;

    @Builder.Default
    private int monsterSpawns = 1;

    @Builder.Default
    private int cacheCleanup = 900;

}
