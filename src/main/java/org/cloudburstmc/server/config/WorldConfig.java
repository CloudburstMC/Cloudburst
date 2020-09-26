package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorldConfig {

    private Object seed = null;

    private String generator = null;

    private String options = null;

}
