package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * One pattern layer on a banner: the dye color as today's save code writes it
 * (the dye data masked to four bits) and the pattern name.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
@EqualsAndHashCode
public class BannerPatternData {

    @JsonProperty("Color")
    private int color;

    @JsonProperty("Pattern")
    private String pattern;

    public BannerPatternData() {
    }
}
