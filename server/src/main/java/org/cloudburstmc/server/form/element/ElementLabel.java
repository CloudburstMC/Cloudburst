package org.cloudburstmc.server.form.element;

import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

@ToString
public final class ElementLabel extends Element {

    public ElementLabel(@NonNull String elementText) {
        super(ElementType.LABEL, elementText);
    }
}
