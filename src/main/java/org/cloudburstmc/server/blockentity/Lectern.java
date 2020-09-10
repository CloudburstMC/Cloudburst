package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.ItemStack;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

public interface Lectern extends BlockEntity {

    boolean hasBook();

    @Nullable
    ItemStack getBook();

    void setBook(@Nullable ItemStack book);

    int getPage();

    void setPage(@Nonnegative int page);

    int getTotalPages();
}
