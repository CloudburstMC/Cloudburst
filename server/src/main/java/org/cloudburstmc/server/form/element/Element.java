package org.cloudburstmc.server.form.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Element {

    @JsonProperty("type")
    private final ElementType elementType;
    @JsonProperty("text")
    private String elementText;

    Element(@NonNull ElementType elementType, @NonNull String elementText) {
        Preconditions.checkNotNull(elementType, "The provided element type can not be null");
        Preconditions.checkNotNull(elementText, "The provided element text can not be null");

        this.elementType = elementType;
        this.elementText = elementText;
    }

    @NonNull
    public ElementType getElementType() {
        return this.elementType;
    }

    @NonNull
    public String getElementText() {
        return this.elementText;
    }

    @NonNull
    public Element text(@NonNull String elementText) {
        Preconditions.checkNotNull(elementText, "The provided element text can not be null");
        this.elementText = elementText;
        return this;
    }
}
