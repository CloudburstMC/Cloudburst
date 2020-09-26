package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicksPerConfig {

    private int autosave = 6000;

    private int animalSpawns = 400;

    private int monsterSpawns = 1;

    private int cacheCleanup = 900;

}
