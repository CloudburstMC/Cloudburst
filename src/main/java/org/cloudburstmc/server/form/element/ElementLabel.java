package org.cloudburstmc.server.form.element;

import lombok.ToString;

import javax.annotation.Nonnull;

@ToString
public final class ElementLabel extends Element {

    public ElementLabel(@Nonnull String elementText) {
        super(ElementType.LABEL, elementText);
    }
}
