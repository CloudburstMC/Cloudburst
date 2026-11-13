package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudLecternData extends CloudBlockEntityData {

    @JsonProperty("hasBook")
    private Boolean hasBook;

    @JsonProperty("book")
    private ItemStack book;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("totalPages")
    private Integer totalPages;
}
