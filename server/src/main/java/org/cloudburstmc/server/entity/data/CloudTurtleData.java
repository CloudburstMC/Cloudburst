package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudTurtleData extends CloudCreatureData {

    @JsonProperty("Age")
    private int age;

    @JsonIgnore
    private Vector3i homePosition = Vector3i.ZERO;
}
