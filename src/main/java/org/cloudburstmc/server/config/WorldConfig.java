package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorldConfig {

    @Builder.Default
    private Object seed = null;

    @Builder.Default
    private String generator = null;

    @Builder.Default
    private String options = null;

}
