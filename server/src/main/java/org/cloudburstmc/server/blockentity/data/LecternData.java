package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Persistent data of a lectern: the book it holds and the displayed page.
 * The keys are only written while a book is present.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LecternData extends BlockEntityData {

    @JsonProperty("hasBook")
    private Boolean hasBook;

    @JsonProperty("book")
    private ItemStack book;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("totalPages")
    private Integer totalPages;
}
